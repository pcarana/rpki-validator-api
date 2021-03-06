package mx.nic.lab.rpki.api.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import mx.nic.lab.rpki.api.config.ApiConfiguration;
import mx.nic.lab.rpki.api.exception.BadRequestException;
import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.exception.NotFoundException;
import mx.nic.lab.rpki.api.exception.TrustAnchorExtractorException;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.pojo.TalUri;
import net.ripe.rpki.commons.rsync.Command;

/**
 * Utilery class
 *
 */
public class Util {

	/**
	 * Common format to return dates as String
	 */
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HHmmss.SSS'Z'";

	/**
	 * List of available bundles ordered by priority (if the first has the searched
	 * key, then return that value; otherwise keep searching at the next, and so on)
	 */
	private static final List<String> bundles = Arrays.asList("labels/errors", "validation", "labels/validation");

	/**
	 * Return the additional path info of a request URI as a String List, i.e. If
	 * the request's URI is "/server/ip/192.0.2.0/24", and the servlet path where
	 * this request was received is "/server/ip/*", then this returns the list
	 * ["192.0.2.0", "24"].
	 * 
	 * @param request
	 *            request you want the arguments from.
	 * @param maxParamsExpected
	 *            maximum number of parameters expected, negative value means
	 *            indefinite
	 * @param allowEmptyPath
	 *            boolean to prove if an empty path is allowed
	 * @return <code>List</code> with additional path info.
	 * @throws HttpException
	 *             <code>request</code> is not a valid URI.
	 */
	public static List<String> getAdditionaPathInfo(HttpServletRequest request, int maxParamsExpected,
			boolean allowEmptyPath) throws HttpException {
		String pathInfo = request.getPathInfo();
		if (pathInfo == null || pathInfo.equals("/")) {
			if (!allowEmptyPath) {
				throw new BadRequestException("#{error.missingArguments}");
			}
			return Collections.emptyList();
		}
		// Ignores the first "/"
		String[] stringArr = pathInfo.substring(1).split("/");
		List<String> requestParams = Arrays.asList(stringArr);
		// If maxParamsExpected is sent then validate against its value
		if (maxParamsExpected >= 0 && requestParams.size() > maxParamsExpected) {
			throw new NotFoundException(request.getRequestURI());
		}
		// Check empty strings
		for (String param : requestParams) {
			if (param.isEmpty()) {
				throw new BadRequestException("#{error.missingArguments}");
			}
		}
		return requestParams;
	}

	/**
	 * Concatenates the parameter values expected at the indicated label, the final
	 * result is the String: <code>label{param1}{param2}...{paramN}</code><br>
	 * Later this should be used to replace the parameters with its corresponding
	 * value.
	 * 
	 * @param label
	 *            a label ID (it must exist in a bundle)
	 * @param params
	 *            the parameters to concatenate
	 * @return The label ID with the parameters concatenated
	 */
	public static String concatenateParamsToLabel(String label, Object... params) {
		StringBuilder sb = new StringBuilder();
		sb.append(label);
		if (params.length > 0) {
			for (Object param : params) {
				sb.append("{");
				sb.append(param);
				sb.append("}");
			}
		}
		return sb.toString();
	}

	/**
	 * Get the date as a formatted String
	 * 
	 * @param date
	 * @return
	 */
	public static String getFormattedDate(Date date) {
		SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
		return df.format(date);
	}

	/**
	 * Get an instance from the received {@link HttpServletRequest}, only 3 query
	 * parameters are expected: limit, offset and sort; where:<br>
	 * <ul>
	 * <li>limit: must be a positive integer (excluding 0)
	 * <li>offset: must be a positive integer (including 0) and can't be present if
	 * limit isn't valid nor existent
	 * <li>sort: comma separated values of the properties used for sorting, a
	 * preceding sign can be used to indicate the ordering term ('+' means 'asc',
	 * '-' means 'desc'), if no sign is used then the default value is 'asc'. The
	 * sent order of the parameters is honored. E.g. "?sort=+id,-date,name" means
	 * that the ordering will be 'id asc, date desc, name asc'. The
	 * <code>validSortKeysMap</code> is used to validate that the properties sent
	 * are expected by the corresponding servlet.
	 * </ul>
	 * None of the query parameters is mandatory, but if they're sent then they must
	 * be valid. An empty value is treated as invalid.
	 * 
	 * @param request
	 *            Request from the client
	 * @param validSortKeysMap
	 *            Used to validate the properties that can be used to sort, the key
	 *            is the name of the expected parameter and the value is the
	 *            corresponding POJO property that the DA implementation can use to
	 *            effectively sort
	 * @return a new {@link PagingParameters} instance
	 * @throws BadRequestException
	 *             if the query parameters aren't as expected (empty parameters
	 *             aren't valid)
	 */
	public static PagingParameters createFromRequest(HttpServletRequest request, Map<String, String> validSortKeysMap,
			Map<String, String> validFilterKeysMap) throws BadRequestException {
		// Query parameter name to indicate limit
		final String LIMIT = "limit";
		// Query parameter name to indicate offset
		final String OFFSET = "offset";
		// Query parameter name to indicate the properties used to sort as well as the
		// order (asc, desc)
		final String SORT = "sort";
		// Query parameter name to indicate the field where the filter query will be
		// applied
		final String FILTER_FIELD = "filterField";
		// Query parameter name to indicate the filter query to be applied
		final String FILTER_QUERY = "filterQuery";

		// If sent multiple times, than only take into account the first parameter
		String rcvdLimit = request.getParameter(LIMIT);
		String rcvdOffset = request.getParameter(OFFSET);
		String rcvdSort = request.getParameter(SORT);
		String rcvdFilterField = request.getParameter(FILTER_FIELD);
		String rcvdFilterQuery = request.getParameter(FILTER_QUERY);

		PagingParameters pagingParameters = new PagingParameters();
		Integer maxResponseResults = ApiConfiguration.getMaxResponseResults();
		if (rcvdLimit != null) {
			int intLimit;
			try {
				intLimit = Integer.parseInt(rcvdLimit);
			} catch (NumberFormatException e) {
				throw new BadRequestException(
						Util.concatenateParamsToLabel("#{error.invalid.dataType}", LIMIT, "integer"));
			}
			// Only values greater than 0 are accepted, the 0 value (no limit) can't be used
			// by the client
			if (intLimit < 1) {
				throw new BadRequestException(Util.concatenateParamsToLabel("#{error.paging.minValue}", LIMIT, "1"));
			}
			// The limit isn't greater than the max (considering the 0 means 'no limit')
			if (intLimit <= maxResponseResults || maxResponseResults == 0) {
				pagingParameters.setLimit(intLimit);
			} else {
				pagingParameters.setLimit(maxResponseResults);
			}
		} else {
			pagingParameters.setLimit(maxResponseResults);
		}
		// limit can be received without offset
		if (rcvdOffset != null) {
			try {
				pagingParameters.setOffset(Integer.parseInt(rcvdOffset));
			} catch (NumberFormatException e) {
				throw new BadRequestException(
						Util.concatenateParamsToLabel("#{error.invalid.dataType}", OFFSET, "integer"));
			}
			// ...but offset can't be received without limit
			if (rcvdLimit == null) {
				throw new BadRequestException("#{error.paging.offsetWithoutLimit}");
			}
			// Only positive values are accepted (including 0)
			if (pagingParameters.getOffset() < 0) {
				throw new BadRequestException(Util.concatenateParamsToLabel("#{error.paging.minValue}", OFFSET, "0"));
			}
		}
		if (rcvdSort != null && validSortKeysMap != null) {
			if (rcvdSort.trim().isEmpty()) {
				throw new BadRequestException("#{error.paging.emptySort}");
			}
			String[] rcvdCols = rcvdSort.trim().split(",");
			LinkedHashMap<String, String> paramMap = new LinkedHashMap<>();
			for (String col : rcvdCols) {
				col = col.trim();
				if (col.isEmpty()) {
					throw new BadRequestException("#{error.paging.emptySort}");
				}
				char sign = col.charAt(0);
				String ordering = sign == '-' ? PagingParameters.ORDER_DESC : PagingParameters.ORDER_ASC;
				String key = sign == '-' || sign == '+' ? col.substring(1) : col;
				// Validate the sort keys with the expected
				if (!validSortKeysMap.containsKey(key)) {
					throw new BadRequestException(
							Util.concatenateParamsToLabel("#{error.paging.invalidColumnSort}", key));
				}
				// Check for duplicates
				if (paramMap.containsKey(validSortKeysMap.get(key))) {
					throw new BadRequestException(Util.concatenateParamsToLabel("#{error.paging.duplicatedSort}", key));
				}
				paramMap.put(validSortKeysMap.get(key), ordering);
			}
			if (!paramMap.isEmpty()) {
				pagingParameters.setSort(paramMap);
			}
		}
		// The servlet is expecting filter params
		if (validFilterKeysMap != null) {
			// If sent, The filter params must be together
			if (rcvdFilterField != null && rcvdFilterQuery == null
					|| rcvdFilterField == null && rcvdFilterQuery != null) {
				throw new BadRequestException("#{error.paging.filterParams}");
			}
			if (rcvdFilterField != null) {
				if (rcvdFilterField.trim().isEmpty()) {
					throw new BadRequestException("#{error.paging.emptyFilterField}");
				}
				rcvdFilterField = rcvdFilterField.trim();
				// Validate the sort keys with the expected
				if (!validFilterKeysMap.containsKey(rcvdFilterField)) {
					throw new BadRequestException(
							Util.concatenateParamsToLabel("#{error.paging.invalidColumnFilter}", rcvdFilterField));
				}
				pagingParameters.setFilterField(validFilterKeysMap.get(rcvdFilterField));
			}
			if (rcvdFilterQuery != null) {
				if (rcvdFilterQuery.trim().isEmpty()) {
					throw new BadRequestException("#{error.paging.emptyFilterQuery}");
				}
				rcvdFilterQuery = rcvdFilterQuery.trim();
				pagingParameters.setFilterQuery(rcvdFilterQuery);
			}
		}
		return pagingParameters;
	}

	/**
	 * Load a TAL from a "*.tal" file
	 * 
	 * @param file
	 * @return A {@link Tal} created from the file
	 * @throws TrustAnchorExtractorException
	 *             when the object couldn't be loaded
	 */
	public static Tal loadTalfromFile(File file) throws TrustAnchorExtractorException {
		try {
			String contents = Files.toString(file, Charsets.UTF_8);
			String trimmed = contents.trim();
			if (!looksLikeUri(trimmed)) {
				throw new IllegalArgumentException("First line isn't a valid URI");
			}
			return readStandardTrustAnchorLocator(file.getName(), trimmed);
		} catch (IllegalArgumentException | URISyntaxException | IOException e) {
			throw new TrustAnchorExtractorException(
					"failed to load trust anchor locator " + file + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Get the complete JSON string, replacing all the labels "#{label}" with its
	 * corresponding locale value. If there's no bundle available or no property
	 * defined, an empty String is used to replace the corresponding label.<br>
	 * <br>
	 * If the label has parameters concatenated (e.g. #{label}{param1}{param2} see
	 * more at
	 * {@link mx.nic.lab.rpki.api.util.Util#concatenateParamsToLabel(String, Object...)})
	 * then take those values into account to replace any parameters indicated at
	 * the label. The order of the parameter affects the replacement order, so the
	 * parameter <code>{0}</code> will be replaced with the first param value, the
	 * <code>{1}</code> with the second, and so on...
	 * 
	 * @param locale
	 * @param jsonString
	 * @return JSON string with labels replaced
	 */
	public static String getJsonWithLocale(Locale locale, String jsonString) {
		if (jsonString == null) {
			return jsonString;
		}
		// Match by groups, the 4th group determines the key to lookup at the bundles
		// and the parameters values (if they are present)
		String labelPattern = "(\")((\\#\\{)([^\"]+)(\\}))(\")";
		Matcher labelMatcher = Pattern.compile(labelPattern).matcher(jsonString);
		try {
			String replacement;
			while (labelMatcher.find()) {
				String[] values = labelMatcher.group(4).split("\\}\\{");
				String key = values[0];
				replacement = getValueFromBundles(key, locale);
				// Check for parameters and store its value
				if (values.length > 1) {
					List<String> parameterValues = new ArrayList<>();
					for (int i = 1; i < values.length; i++) {
						parameterValues.add(values[i]);
					}
					replacement = MessageFormat
							.format(replacement, parameterValues.toArray(new Object[parameterValues.size()])).trim();
				}
				replacement = "\"" + replacement + "\"";
				jsonString = jsonString.replace(labelMatcher.group(), replacement);
			}

		} catch (MissingResourceException e) {
			// Not even a default bundle was found (that's bad), this is an internal error,
			// replace the labels with empty strings and log
			Logger.getGlobal().log(Level.SEVERE, "Error loading bundle, still replacing labels", e);
			while (labelMatcher.find()) {
				jsonString = jsonString.replace(labelMatcher.group(), "\"\"");
			}
		}
		return jsonString;
	}

	/**
	 * Creates and executes the command (with optional arguments) using the
	 * {@link Command} object
	 * 
	 * @param commandWArgs
	 *            List including the command to execute and the args used by the
	 *            command
	 * @return A {@link Command} that holds data about the command execution result
	 */
	public static Command createAndExecCommand(List<String> commandWArgs) {
		Command cmd = new Command(commandWArgs);
		cmd.execute();
		return cmd;
	}

	/**
	 * @see <a href="https://tools.ietf.org/html/rfc7730">RFC 7730</a>
	 */
	private static Tal readStandardTrustAnchorLocator(String fileName, String contents)
			throws URISyntaxException, IOException {
		String caName = fileName.replace(".tal", "");
		final ArrayList<URI> certificateLocations = new ArrayList<>();
		try (final BufferedReader reader = new BufferedReader(new StringReader(contents))) {
			String line;
			while ((line = reader.readLine()) != null) {
				final String trimmed = line.trim();
				if (looksLikeUri(trimmed)) {
					certificateLocations.add(new URI(trimmed));
				} else
					break;
			}

			if (line == null)
				throw new IllegalArgumentException("publicKeyInfo not found in TAL file " + fileName);

			StringBuilder publicKeyInfo = new StringBuilder(line.trim());
			while ((line = reader.readLine()) != null) {
				publicKeyInfo.append(line.trim());
			}
			Tal newTal = new Tal();
			newTal.setName(caName);
			newTal.setPublicKey(publicKeyInfo.toString());
			certificateLocations.forEach((uri) -> {
				TalUri talUri = new TalUri();
				talUri.setLocation(uri.toString());
				newTal.getTalUris().add(talUri);
			});
			return newTal;
		}
	}

	private static boolean looksLikeUri(String string) {
		return string.startsWith("rsync://") || string.startsWith("https://") || string.startsWith("http://");
	}

	/**
	 * Search the indicated <code>key</code> at the configured bundles, throws a
	 * {@link MissingResourceException} if nothing was found
	 * 
	 * @param key
	 * @param locale
	 * @return
	 */
	private static String getValueFromBundles(String key, Locale locale) {
		String found;
		for (String location : bundles) {
			found = getValueFromSingleBundle(location, key, locale);
			if (found != null) {
				return found;
			}
		}
		throw new MissingResourceException("None of the bundles " + bundles + " had the key '" + key + "'",
				String.class.getName(), key);
	}

	/**
	 * Search the <code>key</code> at the bundle loaded from the
	 * <code>location</code>, if the bundle with the <code>locale</code> isn't found
	 * then try to get it with the default locale; if nothing is found, return
	 * <code>null</code>
	 * 
	 * @param location
	 * @param key
	 * @param locale
	 * @return
	 */
	private static String getValueFromSingleBundle(String location, String key, Locale locale) {
		try {
			ResourceBundle bundle = ResourceBundle.getBundle(location, locale);
			if (bundle.containsKey(key)) {
				return bundle.getString(key);
			}
		} catch (MissingResourceException e) {
			// Fallback: if no bundle was found, try to use the default
			if (!locale.equals(Locale.getDefault())) {
				return getValueFromSingleBundle(location, key, Locale.getDefault());
			}
		}
		// Nothing found return null
		return null;
	}
}

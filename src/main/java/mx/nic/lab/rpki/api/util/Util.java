package mx.nic.lab.rpki.api.util;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.BadRequestException;
import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.exception.NotFoundException;
import mx.nic.lab.rpki.db.pojo.ApiObject;
import mx.nic.lab.rpki.db.pojo.PagingParameters;

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
	public static PagingParameters createFromRequest(HttpServletRequest request, Map<String, String> validSortKeysMap)
			throws BadRequestException {
		// Query parameter name to indicate limit
		final String LIMIT = "limit";
		// Query parameter name to indicate offset
		final String OFFSET = "offset";
		// Query parameter name to indicate the properties used to sort as well as the
		// order (asc, desc)
		final String SORT = "sort";

		// If sent multiple times, than only take into account the first parameter
		String rcvdLimit = request.getParameter(LIMIT);
		String rcvdOffset = request.getParameter(OFFSET);
		String rcvdSort = request.getParameter(SORT);

		PagingParameters pagingParameters = new PagingParameters();
		if (rcvdLimit != null) {
			try {
				pagingParameters.setLimit(Integer.parseInt(rcvdLimit));
			} catch (NumberFormatException e) {
				throw new BadRequestException(
						Util.concatenateParamsToLabel("#{error.invalid.dataType}", LIMIT, "integer"));
			}
			// Only values greater than 0 are accepted
			if (pagingParameters.getLimit() < 1) {
				throw new BadRequestException(Util.concatenateParamsToLabel("#{error.paging.minValue}", LIMIT, "1"));
			}
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
			if (pagingParameters.getLimit() == -1) {
				throw new BadRequestException("#{error.paging.offsetWithoutLimit}");
			}
			// Only positive values are accepted (including 0)
			if (pagingParameters.getOffset() < 0) {
				throw new BadRequestException(Util.concatenateParamsToLabel("#{error.paging.minValue}", OFFSET, "0"));
			}
		}
		if (rcvdSort != null) {
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
				String ordering = sign == '-' ? ApiObject.ORDER_DESC : ApiObject.ORDER_ASC;
				String key = sign == '-' || sign == '+' ? col.substring(1) : col;
				// Validate the sort keys with the expected
				if (!validSortKeysMap.containsKey(key)) {
					throw new BadRequestException(
							Util.concatenateParamsToLabel("#{error.paging.invalidColumnSort}", key));
				}
				// Check for duplicates
				if (paramMap.containsKey(key)) {
					throw new BadRequestException(Util.concatenateParamsToLabel("#{error.paging.duplicatedSort}", key));
				}
				paramMap.put(validSortKeysMap.get(key), ordering);
			}
			if (!paramMap.isEmpty()) {
				pagingParameters.setSort(paramMap);
			}
		}
		return pagingParameters;
	}
}

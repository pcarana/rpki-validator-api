package mx.nic.lab.rpki.api.servlet;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.exception.InternalServerErrorException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.error.ErrorResult;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.ValidationException;

/**
 * Base class of all API servlets, implements all the supported request methods
 *
 */
public abstract class ApiServlet extends HttpServlet {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(ApiServlet.class.getName());

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
	 * <code>{1}</code> with the second, and so on... <br>
	 * <br>
	 * Finally, the <code>response</code> is modified adding the 'Content-Language'
	 * header to indicate which language was used.
	 * 
	 * @param locale
	 * @param jsonString
	 * @param response
	 * @return JSON string with labels replaced
	 */
	private String getLocaleJson(Locale locale, String jsonString, HttpServletResponse response) {
		if (jsonString == null) {
			return jsonString;
		}
		// Match by groups, the 4th group determines the key to lookup at the bundles
		// and the parameters values (if they are present)
		String labelPattern = "(\")((\\#\\{)([^\"]+)(\\}))(\")";
		Matcher labelMatcher = Pattern.compile(labelPattern).matcher(jsonString);
		ResourceBundle bundle = null;
		try {
			bundle = ResourceBundle.getBundle("META-INF/labels/errors", locale);
			String replacement;
			while (labelMatcher.find()) {
				String[] values = labelMatcher.group(4).split("\\}\\{");
				String key = values[0];
				replacement = bundle.containsKey(key) ? bundle.getString(key) : "";
				// Check for parameters and store its value
				if (values.length > 1) {
					List<String> parameterValues = new ArrayList<>();
					for (int i = 1; i < values.length; i++) {
						parameterValues.add(values[i]);
					}
					replacement = MessageFormat.format(replacement,
							parameterValues.toArray(new Object[parameterValues.size()]));
				}
				replacement = "\"" + replacement + "\"";
				jsonString = jsonString.replace(labelMatcher.group(), replacement);
			}
			response.setHeader("Content-Language", locale.toLanguageTag());
		} catch (MissingResourceException e) {
			// Fallback: if no bundle was found, try to use the default
			if (!locale.equals(Locale.getDefault())) {
				return getLocaleJson(Locale.getDefault(), jsonString, response);
			}
			// Not even the default was found (that's bad), so this is an internal error,
			// replace the labels with empty strings and log
			logger.log(Level.SEVERE, "Error loading bundle, still responding to the request", e);
			while (labelMatcher.find()) {
				jsonString = jsonString.replace(labelMatcher.group(), "\"\"");
			}
		}
		return jsonString;
	}

	/**
	 * Generic handle of all supported requests, gets the {@link ApiResult} and
	 * builds the response (error or success) to send
	 * 
	 * @param requestMethod
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	private void handleRequest(RequestMethod requestMethod, HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		ApiResult result;
		try {
			result = doApiRequest(requestMethod, req);
		} catch (HttpException e) {
			// Handled error, the result will be the exception sent
			result = new ErrorResult(e);
		} catch (ApiDataAccessException e) {
			// Some error sent by the implementation, handle properly
			if (e instanceof ValidationException) {
				result = new ErrorResult((ValidationException) e);
			} else {
				result = new ErrorResult(new InternalServerErrorException());
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		if (result == null) {
			result = new ErrorResult(HttpServletResponse.SC_NOT_FOUND);
		}
		// No code was explicitly assigned, assume an OK response
		if (result.getCode() == 0) {
			result.setCode(HttpServletResponse.SC_OK);
		}

		// Render RESULT
		resp.setStatus(result.getCode());
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("application/json");
		resp.setHeader("Access-Control-Allow-Origin", "*");

		if (result.toJsonStructure() != null) {
			String body = getLocaleJson(req.getLocale(), result.toJsonStructure().toString(), resp);
			resp.getWriter().print(body);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleRequest(RequestMethod.GET, req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleRequest(RequestMethod.POST, req, resp);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleRequest(RequestMethod.PUT, req, resp);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleRequest(RequestMethod.DELETE, req, resp);
	}

	/**
	 * Handles the request and builds a response. Each servlet that extends the
	 * {@link ApiServlet} will handle the supported methods. The response will be
	 * built for you at {@link ApiServlet}.
	 * 
	 * @param requestMethod
	 *            the request Method
	 * @param request
	 *            request to the servlet.
	 * @return response to the user.
	 * @throws HttpException
	 *             Http errors found handling `request`.
	 * @throws ApiDataAccessException
	 *             Generic errors from the data access
	 */
	protected abstract ApiResult doApiRequest(RequestMethod requestMethod, HttpServletRequest request)
			throws HttpException, ApiDataAccessException;

}

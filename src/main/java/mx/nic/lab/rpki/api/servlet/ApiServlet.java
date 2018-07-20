package mx.nic.lab.rpki.api.servlet;

import java.io.IOException;
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

import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.ExceptionResult;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.http.HttpException;
import mx.nic.lab.rpki.db.exception.http.NotFoundException;
import mx.nic.lab.rpki.db.pojo.ApiException;

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
	 * defined, an empty String is used to replace the corresponding label.
	 * 
	 * @param locale
	 * @param jsonString
	 * @return JSON string with labels replaced
	 */
	private String getLocaleJson(Locale locale, String jsonString) {
		if (jsonString == null) {
			return jsonString;
		}
		// Match by groups, the 2nd group determines the key to lookup at the bundles
		String labelPattern = "(\\#\\{)([\\w\\.\\-]+)(\\})";
		Matcher m = Pattern.compile(labelPattern).matcher(jsonString);
		ResourceBundle bundle = null;
		try {
			bundle = ResourceBundle.getBundle("META-INF/labels/errors", locale);
			String replacement;
			while (m.find()) {
				replacement = bundle.containsKey(m.group(2)) ? bundle.getString(m.group(2)) : "";
				jsonString = jsonString.replace(m.group(), replacement);
			}
		} catch (MissingResourceException e) {
			// Fallback: if no bundle was found, try to use the default
			if (!locale.equals(Locale.getDefault())) {
				return getLocaleJson(Locale.getDefault(), jsonString);
			}
			// Not even the default was found (that's bad), so this is an internal error,
			// replace the labels with empty strings and log
			logger.log(Level.SEVERE, "Error loading bundle, still responding to the request", e);
			while (m.find()) {
				jsonString = jsonString.replace(m.group(), "");
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
		int responseCode = HttpServletResponse.SC_OK;
		ApiResult result;
		try {
			result = doApiRequest(requestMethod, req);
		} catch (HttpException e) {
			// Handled error, the result will be the exception sent
			responseCode = e.getHttpResponseStatusCode();
			result = new ExceptionResult(e);
		} catch (ApiDataAccessException e) {
			// Some error sent by the implementation, handle properly
			responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			result = new ExceptionResult(new HttpException(responseCode, "#{exception.internalError}"));
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		if (result == null) {
			NotFoundException nfe = new NotFoundException();
			responseCode = nfe.getHttpResponseStatusCode();
			result = new ExceptionResult(nfe);
		} else if (result instanceof ExceptionResult) {
			// It was an error handled by the ExceptionServlet
			ExceptionResult er = (ExceptionResult) result;
			ApiException ce = (ApiException) er.getApiObject();
			responseCode = ce.getErrorCode();
		}

		// Render RESULT
		resp.setStatus(responseCode);
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("application/json");
		resp.setHeader("Access-Control-Allow-Origin", "*");

		String body = getLocaleJson(req.getLocale(), result.toJsonStructure().toString());
		resp.getWriter().print(body);
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

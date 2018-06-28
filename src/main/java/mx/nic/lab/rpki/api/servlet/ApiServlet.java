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

	private String getLocaleJson(Locale locale, String jsonString) {
		String labelPattern = "(\\#\\{)([\\w\\.\\-]+)(\\})";
		Matcher m = Pattern.compile(labelPattern).matcher(jsonString);
		ResourceBundle bundle = null;
		try {
			bundle = ResourceBundle.getBundle("META-INF/labels/errors", locale);
			while (m.find()) {
				jsonString = jsonString.replace(m.group(), bundle.getString(m.group(2)));
			}
		} catch (MissingResourceException e) {
			// This is an internal error, replace the labels with empty strings and log
			logger.log(Level.SEVERE, "Error loading bundle, still responding to the request", e);
			while (m.find()) {
				jsonString = jsonString.replace(m.group(), "");
			}
		}
		return jsonString;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		int responseCode = HttpServletResponse.SC_OK;
		ApiResult result;
		try {
			result = doApiGet(req);
		} catch (HttpException e) {
			// Handled error, the result will be the exception sent
			responseCode = e.getHttpResponseStatusCode();
			result = new ExceptionResult(e);
		} catch (ApiDataAccessException e) {
			// Some error sent by the implementation, handle properly
			responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			result = new ExceptionResult(new HttpException(responseCode, e.getMessage()));
		}

		if (result == null) {
			NotFoundException nfe = new NotFoundException();
			responseCode = nfe.getHttpResponseStatusCode();
			result = new ExceptionResult(nfe);
		}

		// Render RESULT
		resp.setStatus(responseCode);
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("application/json");
		resp.setHeader("Access-Control-Allow-Origin", "*");

		String body = getLocaleJson(req.getLocale(), result.toJson());
		resp.getWriter().print(body);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

	}

	/**
	 * Handles the `request` GET request and builds a response. Think of it as a
	 * {@link HttpServlet#doGet(HttpServletRequest, HttpServletResponse)}, except
	 * the response will be built for you.
	 * 
	 * @param request
	 *            request to the servlet.
	 * @return response to the user.
	 * @throws HttpException
	 *             Http errors found handling `request`.
	 * @throws ApiDataAccessException
	 *             Generic errors from the data access
	 */
	protected abstract ApiResult doApiGet(HttpServletRequest request) throws HttpException, ApiDataAccessException;
}

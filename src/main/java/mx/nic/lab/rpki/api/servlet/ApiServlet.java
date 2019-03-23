package mx.nic.lab.rpki.api.servlet;

import mx.nic.lab.rpki.api.exception.*;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.ApiResultAbstract;
import mx.nic.lab.rpki.api.result.error.ErrorResult;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.ValidationException;
import mx.nic.lab.rpki.db.pojo.PagingParameters;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class of all API servlets, implements all the supported request methods
 *
 */
public abstract class ApiServlet extends HttpServlet {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Class logger
	 */
	private static final Logger logger = Logger.getLogger(ApiServlet.class.getName());

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
		ApiResultAbstract result;
		try {
			// Validate encoding
			try {
				URLDecoder.decode(req.getRequestURI(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new BadRequestException("#{error.notUtfEncoded}", e);
			}
			// Validate supported methods
			if (getSupportedRequestMethods() == null || !getSupportedRequestMethods().contains(requestMethod)) {
				throw new MethodNotAllowedException();
			}
			result = doApiRequest(requestMethod, req);
		} catch (HttpException e) {
			// Handled error, the result will be the exception sent
			result = new ErrorResult(e);
			if (e instanceof InternalServerErrorException) {
				InternalServerErrorException ex = (InternalServerErrorException) e;
				if (ex.getCause() != null) {
					logger.log(Level.SEVERE, ex.getMessage(), ex);
				} else {
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
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
			result = new ErrorResult(new NotFoundException());
		}
		// No code was explicitly assigned, assume an OK response
		if (result.getCode() == 0) {
			result.setCode(HttpServletResponse.SC_OK);
		}

		result.printBody(req.getLocale(),resp);
	}

	/**
	 * Get the received paging parameters, defined here so that the servlet that
	 * need it can use it
	 * 
	 * @param request
	 * @return
	 * @throws BadRequestException
	 */
	protected PagingParameters getPagingParameters(HttpServletRequest request) throws BadRequestException {
		return Util.createFromRequest(request, getValidSortKeys(request), getValidFilterKeys(request));
	}

	/**
	 * Returns the supported request methods by the servlet. If the servlet supports
	 * multiple methods, then it's its responsibility to handle the behavior
	 * corresponding to each method.
	 * 
	 * @return The list of supported {@link RequestMethod}s
	 */
	protected abstract List<RequestMethod> getSupportedRequestMethods();

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
	protected abstract ApiResultAbstract doApiRequest(RequestMethod requestMethod, HttpServletRequest request)
			throws HttpException, ApiDataAccessException;

	/**
	 * Valid sort keys that can be received as query parameters, they're mapped to
	 * the corresponding POJO properties. The key must be the name of the parameter,
	 * the value must be the name of the POJO property.<br>
	 * <br>
	 * If the servlet supports paging parameters, then it must override this
	 * function, otherwise a <code>null</code> value will be returned to indicate
	 * that the servlet doesn't support sort parameters.
	 * 
	 * @param request
	 * @return Map of the valid query parameters to use as paging parameters
	 */
	protected Map<String, String> getValidSortKeys(HttpServletRequest request) {
		return null;
	}

	/**
	 * Valid filter keys that can be received as query parameters, they're mapped to
	 * the corresponding POJO properties. The key must be the name of the parameter,
	 * the value must be the name of the POJO property.<br>
	 * <br>
	 * If the servlet supports filter parameters, then it must override this
	 * function, otherwise a <code>null</code> value will be returned to indicate
	 * that the servlet doesn't support filter parameters.
	 * 
	 * @param request
	 * @return Map of the valid query parameters to use as filter parameters
	 */
	protected Map<String, String> getValidFilterKeys(HttpServletRequest request) {
		return null;
	}
}

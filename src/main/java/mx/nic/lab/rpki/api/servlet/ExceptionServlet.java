package mx.nic.lab.rpki.api.servlet;

import javax.servlet.RequestDispatcher;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mx.nic.lab.rpki.api.result.ExceptionResult;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.http.HttpException;
import mx.nic.lab.rpki.db.exception.http.MethodNotAllowedException;

/**
 * Servlet to catch unhandled exceptions
 *
 */
@WebServlet(name = "exception", urlPatterns = { "/exception/*" })
public class ExceptionServlet extends ApiServlet {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiRequest(RequestMethod requestMethod, HttpServletRequest request)
			throws HttpException, ApiDataAccessException {
		if (!RequestMethod.GET.equals(requestMethod)) {
			throw new MethodNotAllowedException();
		}
		Object object = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		// The servlet was accesed directly
		if (object == null) {
			request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpServletResponse.SC_NOT_FOUND);
			request.setAttribute(RequestDispatcher.ERROR_MESSAGE, request.getRequestURI());
		}
		ApiResult result = new ExceptionResult(request);
		return result;
	}

}

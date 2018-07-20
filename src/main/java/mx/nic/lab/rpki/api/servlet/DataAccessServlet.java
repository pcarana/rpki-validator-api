package mx.nic.lab.rpki.api.servlet;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.http.BadRequestException;
import mx.nic.lab.rpki.db.exception.http.HttpException;
import mx.nic.lab.rpki.db.exception.http.MethodNotAllowedException;
import mx.nic.lab.rpki.db.exception.http.NotFoundException;
import mx.nic.lab.rpki.db.spi.DAO;

/**
 * A servlet that extracts information from a specific DAO.
 */
public abstract class DataAccessServlet<T extends DAO> extends ApiServlet {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Used to initialize {@link #dao} with whatever the servlet is supposed to
	 * serve.
	 */
	protected abstract T initAccessDAO() throws ApiDataAccessException;

	/**
	 * Returns the name of the objects this servlet handles. Used for debugging and
	 * error messages.
	 */
	protected abstract String getServedObjectName();

	/**
	 * Init the correspondent {@link DAO} and run general validations
	 * 
	 * @param request
	 * @param requestMethod
	 * @return The implementation of the DAO to use
	 * @throws HttpException
	 * @throws ApiDataAccessException
	 */
	private T initAndValidate(HttpServletRequest request, RequestMethod requestMethod)
			throws HttpException, ApiDataAccessException {
		T dao = initAccessDAO();
		if (dao == null) {
			throw new NotFoundException("#{exception.notFound}");
		}

		// Validate encoding
		try {
			URLDecoder.decode(request.getRequestURI(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new BadRequestException("#{exception.notUtfEncoded}", e);
		}

		// Validate supported methods
		if (getSupportedRequestMethods() == null || !getSupportedRequestMethods().contains(requestMethod)) {
			throw new MethodNotAllowedException("#{exception.methodNotAllowed}");
		}
		return dao;
	}

	@Override
	protected ApiResult doApiRequest(RequestMethod requestMethod, HttpServletRequest request)
			throws HttpException, ApiDataAccessException {
		T dao = initAndValidate(request, requestMethod);
		return doApiDaRequest(requestMethod, request, dao);
	}

	/**
	 * Adds data-access-specific behavior on top of
	 * {@link #doApiRequest(RequestMethod, HttpServletRequest)}.
	 */
	protected abstract ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, T dao)
			throws HttpException, ApiDataAccessException;

	/**
	 * Returns the supported request methods by the servlet. If the servlet supports
	 * multiple methods, then it's its responsibility to handle the behavior
	 * corresponding to each method.
	 * 
	 * @return The list of supported {@link RequestMethod}s
	 */
	protected abstract List<RequestMethod> getSupportedRequestMethods();
}

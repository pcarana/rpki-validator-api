package mx.nic.lab.rpki.api.servlet;

import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.exception.NotFoundException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
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
			throw new NotFoundException();
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

}

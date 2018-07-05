package mx.nic.lab.rpki.api.servlet;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.http.BadRequestException;
import mx.nic.lab.rpki.db.exception.http.HttpException;
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

	@Override
	protected ApiResult doApiGet(HttpServletRequest request) throws HttpException, ApiDataAccessException {
		T dao = initAccessDAO();
		if (dao == null) {
			throw new NotFoundException("#{exception.notFound}");
		}

		try {
			URLDecoder.decode(request.getRequestURI(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new BadRequestException("#{exception.notUtfEncoded}", e);
		}

		return doApiDaGet(request, dao);
	}

	/**
	 * Adds data-access-specific validations on top of
	 * {@link #doApiGet(HttpServletRequest)}.
	 */
	protected abstract ApiResult doApiDaGet(HttpServletRequest request, T dao)
			throws HttpException, ApiDataAccessException;

}

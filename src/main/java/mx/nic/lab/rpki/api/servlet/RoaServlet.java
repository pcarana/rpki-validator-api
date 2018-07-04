package mx.nic.lab.rpki.api.servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.result.RoaResult;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.http.HttpException;
import mx.nic.lab.rpki.db.pojo.Roa;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.RoaDAO;

/**
 * Servlet to handle ROAs requests
 *
 */
@WebServlet(name = "roa", urlPatterns = { "/roa/*" })
public class RoaServlet extends DataAccessServlet<RoaDAO> {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected RoaDAO initAccessDAO() throws ApiDataAccessException {
		return DataAccessService.getRoaDAO();
	}

	@Override
	protected String getServedObjectName() {
		return "roa";
	}

	@Override
	protected ApiResult doApiDaGet(HttpServletRequest request, RoaDAO dao)
			throws HttpException, ApiDataAccessException {
		Long id = Long.parseLong(Util.getAdditionaPathInfo(request, 1)[0]);
		Roa roa = dao.getById(id);
		if (roa == null) {
			return null;
		}
		return new RoaResult(roa);
	}

}

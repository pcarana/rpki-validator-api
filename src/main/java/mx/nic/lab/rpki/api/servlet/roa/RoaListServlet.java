package mx.nic.lab.rpki.api.servlet.roa;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.RoaListResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.http.HttpException;
import mx.nic.lab.rpki.db.pojo.Roa;
import mx.nic.lab.rpki.db.spi.RoaDAO;

/**
 * Servlet to provide all the ROAs
 *
 */
@WebServlet(name = "roaList", value = { "/roa" })
public class RoaListServlet extends RoaServlet {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, RoaDAO dao)
			throws HttpException, ApiDataAccessException {
		List<Roa> roas = dao.getAll();
		return new RoaListResult(roas);
	}

	@Override
	protected String getServedObjectName() {
		return "roaList";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.GET);
	}

}

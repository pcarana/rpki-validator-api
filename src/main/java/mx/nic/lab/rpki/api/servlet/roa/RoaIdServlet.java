package mx.nic.lab.rpki.api.servlet.roa;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.BadRequestException;
import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.roa.RoaResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.Roa;
import mx.nic.lab.rpki.db.spi.RoaDAO;

/**
 * Servlet to provide ROAs by its ID
 *
 */
@WebServlet(name = "roaId", urlPatterns = { "/roa/*" })
public class RoaIdServlet extends RoaServlet {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, RoaDAO dao)
			throws HttpException, ApiDataAccessException {
		List<String> additionalPathInfo = Util.getAdditionaPathInfo(request, 1, false);
		Long id = null;
		try {
			id = Long.parseLong(additionalPathInfo.get(0));
		} catch (NumberFormatException e) {
			throw new BadRequestException("#{error.invalidId}", e);
		}
		Roa roa = dao.getById(id);
		if (roa == null) {
			return null;
		}
		return new RoaResult(roa);
	}

	@Override
	protected String getServedObjectName() {
		return "roaId";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.GET);
	}

}

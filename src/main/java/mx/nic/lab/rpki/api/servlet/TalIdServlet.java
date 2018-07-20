package mx.nic.lab.rpki.api.servlet;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.TalResult;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.http.BadRequestException;
import mx.nic.lab.rpki.db.exception.http.HttpException;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.spi.TalDAO;

/**
 * Servlet to provide TALs by its ID
 *
 */
@WebServlet(name = "talId", value = { "/tal/*" })
public class TalIdServlet extends TalServlet {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, TalDAO dao)
			throws HttpException, ApiDataAccessException {
		List<String> additionalPathInfo = Util.getAdditionaPathInfo(request, 1, false);
		Long id = null;
		try {
			id = Long.parseLong(additionalPathInfo.get(0));
		} catch (NumberFormatException e) {
			throw new BadRequestException("#{exception.invalidId}", e);
		}
		Tal tal = dao.getById(id);
		if (tal == null) {
			return null;
		}
		return new TalResult(tal);
	}

	@Override
	protected String getServedObjectName() {
		return "talId";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.GET);
	}

}

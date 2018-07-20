package mx.nic.lab.rpki.api.servlet.tal;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.TalSyncAllResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.http.HttpException;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.spi.TalDAO;

/**
 * Servlet to synchronize all the configured TALs
 *
 */
@WebServlet(name = "talSyncAll", value = { "/tal/sync" })
public class TalSyncAllServlet extends TalServlet {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, TalDAO dao)
			throws HttpException, ApiDataAccessException {
		List<Tal> tals = dao.syncAll();
		return new TalSyncAllResult(tals);
	}

	@Override
	protected String getServedObjectName() {
		return "talSyncAll";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.PUT);
	}

}

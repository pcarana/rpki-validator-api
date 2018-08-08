package mx.nic.lab.rpki.api.servlet.rtr;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.rtr.RtrSessionListResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.RtrSession;
import mx.nic.lab.rpki.db.spi.RtrSessionDAO;

/**
 * Servlet to provide all the RTR sessions
 *
 */
@WebServlet(name = "rtrSessionList", value = { "/rtr" })
public class RtrSessionListServlet extends RtrSessionServlet {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, RtrSessionDAO dao)
			throws HttpException, ApiDataAccessException {
		List<RtrSession> rtrSessions = dao.getAll();
		return new RtrSessionListResult(rtrSessions);
	}

	@Override
	protected String getServedObjectName() {
		return "rtrSessionList";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.GET);
	}

}

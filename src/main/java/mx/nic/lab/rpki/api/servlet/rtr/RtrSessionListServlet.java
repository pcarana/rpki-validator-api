package mx.nic.lab.rpki.api.servlet.rtr;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.rtr.RtrSessionListResult;
import mx.nic.lab.rpki.api.servlet.PagingParameters;
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
	 * Valid sort keys that can be received as query parameters, they're mapped to
	 * the corresponding POJO properties
	 */
	private static final Map<String, String> validSortKeysMap;
	static {
		validSortKeysMap = new HashMap<>();
		validSortKeysMap.put("address", RtrSession.ADDRESS);
		validSortKeysMap.put("port", RtrSession.PORT);
		validSortKeysMap.put("status", RtrSession.STATUS);
		validSortKeysMap.put("lastRequest", RtrSession.LAST_REQUEST);
		validSortKeysMap.put("lastResponse", RtrSession.LAST_RESPONSE);
	}

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, RtrSessionDAO dao)
			throws HttpException, ApiDataAccessException {
		PagingParameters pagingParams = PagingParameters.createFromRequest(request, validSortKeysMap);
		List<RtrSession> rtrSessions = dao.getAll(pagingParams.getLimit(), pagingParams.getOffset(),
				pagingParams.getSort());
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

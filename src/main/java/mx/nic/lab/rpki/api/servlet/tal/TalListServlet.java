package mx.nic.lab.rpki.api.servlet.tal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.tal.TalListResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.spi.TalDAO;

/**
 * Servlet to provide all the configured TALs
 *
 */
@WebServlet(name = "talList", value = { "/tal" })
public class TalListServlet extends TalServlet {

	/**
	 * Valid sort keys that can be received as query parameters, they're mapped to
	 * the corresponding POJO properties
	 */
	private static final Map<String, String> validSortKeysMap;
	static {
		validSortKeysMap = new HashMap<>();
		validSortKeysMap.put("id", Tal.ID);
		validSortKeysMap.put("name", Tal.NAME);
	}

	/**
	 * Valid filter keys that can be received as query parameters, they're mapped to
	 * the corresponding POJO properties
	 */
	private static final Map<String, String> validFilterKeysMap;
	static {
		validFilterKeysMap = new HashMap<>();
		validFilterKeysMap.put("name", Tal.NAME);
	}

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, TalDAO dao)
			throws HttpException, ApiDataAccessException {
		PagingParameters pagingParameters = getPagingParameters(request);
		ListResult<Tal> tals = dao.getAll(pagingParameters);
		return new TalListResult(tals, pagingParameters);
	}

	@Override
	protected String getServedObjectName() {
		return "talList";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.GET);
	}

	@Override
	protected Map<String, String> getValidSortKeys(HttpServletRequest request) {
		return validSortKeysMap;
	}

	@Override
	protected Map<String, String> getValidFilterKeys(HttpServletRequest request) {
		return validFilterKeysMap;
	}

}

package mx.nic.lab.rpki.api.servlet.roa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.roa.RoaListResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.Roa;
import mx.nic.lab.rpki.db.spi.RoaDAO;

/**
 * Servlet to provide all the ROAs
 *
 */
@WebServlet(name = "roaList", value = { "/roa" })
public class RoaListServlet extends RoaServlet {

	/**
	 * Valid sort keys that can be received as query parameters, they're mapped to
	 * the corresponding POJO properties
	 */
	private static final Map<String, String> validSortKeysMap;
	static {
		validSortKeysMap = new HashMap<>();
		validSortKeysMap.put("id", Roa.ID);
		validSortKeysMap.put("asn", Roa.ASN);
		validSortKeysMap.put("prefix", Roa.START_PREFIX);
		validSortKeysMap.put("prefixLength", Roa.PREFIX_LENGTH);
		validSortKeysMap.put("prefixMaxLength", Roa.PREFIX_MAX_LENGTH);
	}

	/**
	 * Valid filter keys that can be received as query parameters, they're mapped to
	 * the corresponding POJO properties
	 */
	private static final Map<String, String> validFilterKeysMap;
	static {
		validFilterKeysMap = new HashMap<>();
		validFilterKeysMap.put("asn", Roa.ASN);
		validFilterKeysMap.put("prefix", Roa.PREFIX_TEXT);
	}
	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, RoaDAO dao)
			throws HttpException, ApiDataAccessException {
		PagingParameters pagingParameters = getPagingParameters(request);
		ListResult<Roa> roas = dao.getAll(pagingParameters);
		return new RoaListResult(roas, pagingParameters);
	}

	@Override
	protected String getServedObjectName() {
		return "roaList";
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

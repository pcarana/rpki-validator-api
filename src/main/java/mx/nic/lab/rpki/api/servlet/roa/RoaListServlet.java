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
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
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
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, RoaDAO dao)
			throws HttpException, ApiDataAccessException {
		PagingParameters pagingParams = Util.createFromRequest(request, validSortKeysMap);
		List<Roa> roas = dao.getAll(pagingParams);
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

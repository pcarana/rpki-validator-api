package mx.nic.lab.rpki.api.servlet.slurm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmPrefixListResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.SlurmPrefix;
import mx.nic.lab.rpki.db.spi.SlurmPrefixDAO;

/**
 * Servlet to provide all the SLURM Prefixes
 *
 */
@WebServlet(name = "slurmPrefixList", value = { "/slurm/prefix" })
public class SlurmPrefixListServlet extends SlurmPrefixServlet {

	/**
	 * Valid sort keys that can be received as query parameters, they're mapped to
	 * the corresponding POJO properties
	 */
	private static final Map<String, String> validSortKeysMap;
	static {
		validSortKeysMap = new HashMap<>();
		validSortKeysMap.put("id", SlurmPrefix.ID);
		validSortKeysMap.put("asn", SlurmPrefix.ASN);
		validSortKeysMap.put("prefix", SlurmPrefix.START_PREFIX);
		validSortKeysMap.put("maxPrefixLength", SlurmPrefix.PREFIX_MAX_LENGTH);
		validSortKeysMap.put("type", SlurmPrefix.TYPE);
	}

	/**
	 * Valid filter keys that can be received as query parameters, they're mapped to
	 * the corresponding POJO properties
	 */
	private static final Map<String, String> validFilterKeysMap;
	static {
		validFilterKeysMap = new HashMap<>();
		validFilterKeysMap.put("asn", SlurmPrefix.ASN);
		validFilterKeysMap.put("prefix", SlurmPrefix.PREFIX_TEXT);
		validFilterKeysMap.put("type", SlurmPrefix.TYPE);
	}

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, SlurmPrefixDAO dao)
			throws HttpException, ApiDataAccessException {
		PagingParameters pagingParameters = getPagingParameters(request);
		ListResult<SlurmPrefix> slurmPrefixes = dao.getAll(pagingParameters);
		return new SlurmPrefixListResult(slurmPrefixes, pagingParameters);
	}

	@Override
	protected String getServedObjectName() {
		return "slurmPrefixList";
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

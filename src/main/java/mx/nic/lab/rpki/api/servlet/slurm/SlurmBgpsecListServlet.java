package mx.nic.lab.rpki.api.servlet.slurm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmBgpsecListResult;
import mx.nic.lab.rpki.api.servlet.PagingParameters;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.SlurmBgpsec;
import mx.nic.lab.rpki.db.spi.SlurmBgpsecDAO;

/**
 * Servlet to provide all the SLURM Bgpsecs
 *
 */
@WebServlet(name = "slurmBgpsecList", value = { "/slurm/bgpsec" })
public class SlurmBgpsecListServlet extends SlurmBgpsecServlet {

	/**
	 * Valid sort keys that can be received as query parameters, they're mapped to
	 * the corresponding POJO properties
	 */
	private static final Map<String, String> validSortKeysMap;
	static {
		validSortKeysMap = new HashMap<>();
		validSortKeysMap.put("id", SlurmBgpsec.ID);
		validSortKeysMap.put("asn", SlurmBgpsec.ASN);
		validSortKeysMap.put("type", SlurmBgpsec.TYPE);
	}

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, SlurmBgpsecDAO dao)
			throws HttpException, ApiDataAccessException {
		PagingParameters pagingParams = PagingParameters.createFromRequest(request, validSortKeysMap);
		List<SlurmBgpsec> slurmBgpsecs = dao.getAll(pagingParams.getLimit(), pagingParams.getOffset(),
				pagingParams.getSort());
		return new SlurmBgpsecListResult(slurmBgpsecs);
	}

	@Override
	protected String getServedObjectName() {
		return "slurmBgpsecList";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.GET);
	}

}

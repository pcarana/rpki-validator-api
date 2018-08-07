package mx.nic.lab.rpki.api.servlet.slurm;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.BadRequestException;
import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmBgpsecListResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmBgpsecResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.SlurmBgpsec;
import mx.nic.lab.rpki.db.spi.SlurmBgpsecDAO;

/**
 * Servlet to:<br>
 * <li>Provide SLURM BGPsec by its ID (GET)
 * <li>Store a new SLURM BGPsec (POST)
 * <li>Delete a SLURM BGPsec by its ID (DELETE)
 *
 */
@WebServlet(name = "slurmBgpsecId", urlPatterns = { "/slurm/bgpsec/*" })
public class SlurmBgpsecIdServlet extends SlurmBgpsecServlet {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, SlurmBgpsecDAO dao)
			throws HttpException, ApiDataAccessException {
		if (RequestMethod.GET.equals(requestMethod)) {
			return handleGet(request, dao);
		}
		if (RequestMethod.POST.equals(requestMethod)) {
			return handlePost(request, dao);
		}
		if (RequestMethod.DELETE.equals(requestMethod)) {
			return handleDelete(request, dao);
		}
		return null;
	}

	@Override
	protected String getServedObjectName() {
		return "slurmBgpsecId";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE);
	}

	/**
	 * Handle a GET request, only 1 parameter is expected, it must be a: integer
	 * (id), "filter", or "assertion". Depending on the request a distinct ApiResult
	 * will be returned (a single result for the "id" search, or a list for the
	 * "filter"/"assertion" request.
	 * 
	 * @param request
	 * @param dao
	 * @return
	 * @throws HttpException
	 * @throws ApiDataAccessException
	 */
	private ApiResult handleGet(HttpServletRequest request, SlurmBgpsecDAO dao)
			throws HttpException, ApiDataAccessException {
		// The GET request only expects 3 possible paths: {id}, "filter", or "assertion"
		List<String> additionalPathInfo = Util.getAdditionaPathInfo(request, 1, false);
		String requestedService = additionalPathInfo.get(0);
		ApiResult result = null;

		// Check if is a filter/assertion request
		if (requestedService.equals(FILTER_SERVICE)) {
			List<SlurmBgpsec> filters = dao.getAllByType(SlurmBgpsec.TYPE_FILTER);
			result = new SlurmBgpsecListResult(filters);
		} else if (requestedService.equals(ASSERTION_SERVICE)) {
			List<SlurmBgpsec> assertions = dao.getAllByType(SlurmBgpsec.TYPE_ASSERTION);
			result = new SlurmBgpsecListResult(assertions);
		} else {
			// Check if is an ID
			Long id = null;
			try {
				id = Long.parseLong(requestedService);
			} catch (NumberFormatException e) {
				throw new BadRequestException("#{error.invalidId}", e);
			}
			SlurmBgpsec slurmBgpsec = dao.getById(id);
			if (slurmBgpsec == null) {
				return null;
			}
			result = new SlurmBgpsecResult(slurmBgpsec);
		}
		return result;
	}

	/**
	 * Handle a POST request, 1 parameter is expected ("filter" or "assertion"), the
	 * body should contain the values to store. A single result will be returned
	 * depending on the result of the store operation.
	 * 
	 * @param request
	 * @param dao
	 * @return
	 * @throws ApiDataAccessException
	 */
	private ApiResult handlePost(HttpServletRequest request, SlurmBgpsecDAO dao) throws ApiDataAccessException {
		// FIXME Complete behavior
		return null;
	}

	/**
	 * Handle a DELETE request, 1 parameter is expected: the ID of the BGPsec to
	 * delete. A single result will be returned depending on the result of the
	 * delete operation.
	 * 
	 * @param request
	 * @param dao
	 * @return
	 * @throws ApiDataAccessException
	 */
	private ApiResult handleDelete(HttpServletRequest request, SlurmBgpsecDAO dao) throws ApiDataAccessException {
		// FIXME Complete behavior
		return null;
	}
}

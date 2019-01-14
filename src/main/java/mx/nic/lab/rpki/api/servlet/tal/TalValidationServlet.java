package mx.nic.lab.rpki.api.servlet.tal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.BadRequestException;
import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.tal.TalValidationResult;
import mx.nic.lab.rpki.api.servlet.DataAccessServlet;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.ValidationCheck;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.ValidationRunDAO;

/**
 * Servlet to provide the last successful validation details of a TAL by its ID
 *
 */
@WebServlet(name = "talValidationId", value = { "/tal/validation/*" })
public class TalValidationServlet extends DataAccessServlet<ValidationRunDAO> {

	/**
	 * Valid sort keys that can be received as query parameters, they're mapped to
	 * the corresponding POJO properties
	 */
	private static final Map<String, String> validSortKeysMap;
	static {
		validSortKeysMap = new HashMap<>();
		validSortKeysMap.put("fileType", ValidationCheck.FILE_TYPE);
		validSortKeysMap.put("status", ValidationCheck.STATUS);
		validSortKeysMap.put("location", ValidationCheck.LOCATION);
	}

	/**
	 * Valid filter keys that can be received as query parameters, they're mapped to
	 * the corresponding POJO properties
	 */
	private static final Map<String, String> validFilterKeysMap;
	static {
		validFilterKeysMap = new HashMap<>();
		validFilterKeysMap.put("fileType", ValidationCheck.FILE_TYPE);
		validFilterKeysMap.put("status", ValidationCheck.STATUS);
		validFilterKeysMap.put("location", ValidationCheck.LOCATION);
	}

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ValidationRunDAO initAccessDAO() throws ApiDataAccessException {
		return DataAccessService.getValidationRunDAO();
	}

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, ValidationRunDAO dao)
			throws HttpException, ApiDataAccessException {
		List<String> additionalPathInfo = Util.getAdditionaPathInfo(request, 1, false);
		Long id = null;
		try {
			id = Long.parseLong(additionalPathInfo.get(0));
		} catch (NumberFormatException e) {
			throw new BadRequestException("#{error.invalidId}", e);
		}
		PagingParameters pagingParameters = getPagingParameters(request);
		ListResult<ValidationCheck> validationChecks = dao.getLastSuccessfulChecksByTal(id, pagingParameters);
		return new TalValidationResult(validationChecks, pagingParameters);
	}

	@Override
	protected String getServedObjectName() {
		return "talValidationId";
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

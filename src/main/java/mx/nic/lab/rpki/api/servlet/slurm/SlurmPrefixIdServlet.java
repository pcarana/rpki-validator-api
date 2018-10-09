package mx.nic.lab.rpki.api.servlet.slurm;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mx.nic.lab.rpki.api.exception.BadRequestException;
import mx.nic.lab.rpki.api.exception.ConflictException;
import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.exception.InternalServerErrorException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.EmptyResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmPrefixListResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmPrefixSingleResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.ValidationError;
import mx.nic.lab.rpki.db.exception.ValidationException;
import mx.nic.lab.rpki.db.pojo.ApiObject;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.SlurmPrefix;
import mx.nic.lab.rpki.db.spi.SlurmPrefixDAO;

/**
 * Servlet to:<br>
 * <li>Provide SLURM Prefix by its ID (GET)
 * <li>Store a new SLURM Prefix (POST)
 * <li>Delete a SLURM Prefix by its ID (DELETE)
 *
 */
@WebServlet(name = "slurmPrefixId", urlPatterns = { "/slurm/prefix/*" })
public class SlurmPrefixIdServlet extends SlurmPrefixServlet {

	/**
	 * Valid sort keys that can be received as query parameters at a filter search,
	 * they're mapped to the corresponding POJO properties
	 */
	private static final Map<String, String> validFilterSortKeysMap;
	static {
		validFilterSortKeysMap = new HashMap<>();
		validFilterSortKeysMap.put("id", SlurmPrefix.ID);
		validFilterSortKeysMap.put("asn", SlurmPrefix.ASN);
		validFilterSortKeysMap.put("prefix", SlurmPrefix.START_PREFIX);
	}

	/**
	 * Valid sort keys that can be received as query parameters at an assertion
	 * search, they're mapped to the corresponding POJO properties
	 */
	private static final Map<String, String> validAssertionSortKeysMap;
	static {
		validAssertionSortKeysMap = new HashMap<>();
		validAssertionSortKeysMap.put("id", SlurmPrefix.ID);
		validAssertionSortKeysMap.put("asn", SlurmPrefix.ASN);
		validAssertionSortKeysMap.put("prefix", SlurmPrefix.START_PREFIX);
		validAssertionSortKeysMap.put("maxPrefixLength", SlurmPrefix.PREFIX_MAX_LENGTH);
	}

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, SlurmPrefixDAO dao)
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
		return "slurmPrefixId";
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
	private ApiResult handleGet(HttpServletRequest request, SlurmPrefixDAO dao)
			throws HttpException, ApiDataAccessException {
		// The GET request only expects 3 possible paths: {id}, "filter", or "assertion"
		List<String> additionalPathInfo = Util.getAdditionaPathInfo(request, 1, false);
		String requestedService = additionalPathInfo.get(0);
		ApiResult result = null;

		// Check if is a filter/assertion request
		if (requestedService.equals(FILTER_SERVICE)) {
			PagingParameters pagingParams = Util.createFromRequest(request, validFilterSortKeysMap);
			List<SlurmPrefix> filters = dao.getAllByType(SlurmPrefix.TYPE_FILTER, pagingParams);
			result = new SlurmPrefixListResult(filters);
		} else if (requestedService.equals(ASSERTION_SERVICE)) {
			PagingParameters pagingParams = Util.createFromRequest(request, validAssertionSortKeysMap);
			List<SlurmPrefix> assertions = dao.getAllByType(SlurmPrefix.TYPE_ASSERTION, pagingParams);
			result = new SlurmPrefixListResult(assertions);
		} else {
			// Check if is an ID
			Long id = null;
			try {
				id = Long.parseLong(requestedService);
			} catch (NumberFormatException e) {
				throw new BadRequestException("#{error.invalidId}", e);
			}
			SlurmPrefix slurmPrefix = dao.getById(id);
			if (slurmPrefix == null) {
				return null;
			}
			result = new SlurmPrefixSingleResult(slurmPrefix);
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
	 * @throws HttpException
	 * @throws ApiDataAccessException
	 */
	private ApiResult handlePost(HttpServletRequest request, SlurmPrefixDAO dao)
			throws HttpException, ApiDataAccessException {
		// Get the service and validate expected body
		List<String> additionalPathInfo = Util.getAdditionaPathInfo(request, 1, false);

		// Check if is a filter/assertion request
		int type;
		String requestedService = additionalPathInfo.get(0);
		if (requestedService.equals(FILTER_SERVICE)) {
			type = SlurmPrefix.TYPE_FILTER;
		} else if (requestedService.equals(ASSERTION_SERVICE)) {
			type = SlurmPrefix.TYPE_ASSERTION;
		} else {
			// Nothing to do, a NotFound Exception will be thrown
			return null;
		}
		SlurmPrefix newSlurmPrefix = getSlurmPrefixFromBody(request, type);
		newSlurmPrefix.setType(type);
		try {
			if (!dao.create(newSlurmPrefix)) {
				throw new ConflictException("#{error.creationIncomplete}");
			}
			EmptyResult result = new EmptyResult();
			result.setCode(HttpServletResponse.SC_CREATED);
			return result;
		} catch (ApiDataAccessException e) {
			if (e instanceof ValidationException) {
				// Some errors must be mapped so that match to the clients request
				ValidationException ve = (ValidationException) e;
				List<ValidationError> removeErrors = new ArrayList<>();
				if (ve.getValidationErrors() != null) {
					ve.getValidationErrors().forEach((error) -> {
						String field = error.getField();
						if (field == null) {
							return;
						}
						if (field.equals(SlurmPrefix.START_PREFIX)) {
							error.setField("prefix");
							return;
						}
						if (field.equals(SlurmPrefix.PREFIX_MAX_LENGTH)) {
							error.setField("maxPrefixLength");
							return;
						}
						if (field.equals(SlurmPrefix.END_PREFIX)) {
							removeErrors.add(error);
							return;
						}
					});
				}
				removeErrors.forEach((error) -> ve.getValidationErrors().remove(error));
			}
			throw e;
		}
	}

	/**
	 * Handle a DELETE request, 1 parameter is expected: the ID of the prefix to
	 * delete. A single result will be returned depending on the result of the
	 * delete operation.
	 * 
	 * @param request
	 * @param dao
	 * @return
	 * @throws HttpException
	 * @throws ApiDataAccessException
	 */
	private ApiResult handleDelete(HttpServletRequest request, SlurmPrefixDAO dao)
			throws HttpException, ApiDataAccessException {
		List<String> additionalPathInfo = Util.getAdditionaPathInfo(request, 1, false);
		// First check that the object exists
		Long id = null;
		try {
			id = Long.parseLong(additionalPathInfo.get(0));
		} catch (NumberFormatException e) {
			throw new BadRequestException("#{error.invalidId}", e);
		}
		if (!dao.deleteById(id)) {
			throw new ConflictException();
		}
		return new EmptyResult();
	}

	/**
	 * Get a {@link SlurmPrefix} from the body request, runs some basic validations
	 * 
	 * @param request
	 * @param type
	 * @return
	 * @throws HttpException
	 */
	private SlurmPrefix getSlurmPrefixFromBody(HttpServletRequest request, int type) throws HttpException {
		SlurmPrefix slurmPrefix = new SlurmPrefix();
		JsonObject object = null;
		try (JsonReader reader = Json.createReader(request.getReader())) {
			object = reader.readObject();
		} catch (IOException e) {
			throw new InternalServerErrorException(e);
		} catch (JsonException | IllegalStateException e) {
			throw new BadRequestException("#{error.invalidJson}");
		}
		// Check for extra keys (invalid keys)
		List<String> invalidKeys = new ArrayList<>();
		for (String key : object.keySet()) {
			if (!key.matches("(prefix|asn|maxPrefixLength|comment)")) {
				invalidKeys.add(key);
			}
		}
		if (!invalidKeys.isEmpty()) {
			throw new BadRequestException(
					Util.concatenateParamsToLabel("#{error.invalid.keys}", invalidKeys.toString()));
		}
		String prefixRcv = null;
		try {
			prefixRcv = object.getString("prefix");
		} catch (NullPointerException npe) {
			if (type == SlurmPrefix.TYPE_ASSERTION) {
				throw new BadRequestException("#{error.slurm.prefix.prefixRequired}");
			}
		} catch (ClassCastException cce) {
			throw new BadRequestException(
					Util.concatenateParamsToLabel("#{error.invalid.dataType}", "prefix", "String"));
		}
		if (prefixRcv != null) {
			String[] prefixArr = prefixRcv.split("/");
			if (prefixArr.length != 2) {
				throw new BadRequestException(
						Util.concatenateParamsToLabel("#{error.invalid.format}", "prefix", "[prefix]/[prefix_length]"));
			}

			try {
				InetAddress prefixAddress = InetAddress.getByName(prefixArr[0]);
				slurmPrefix.setStartPrefix(prefixAddress.getAddress());
				slurmPrefix.setPrefixText(prefixAddress.getHostAddress());
			} catch (UnknownHostException e) {
				throw new BadRequestException("#{error.slurm.prefix.invalid}");
			}
			try {
				int prefixLength = Integer.valueOf(prefixArr[1]);
				slurmPrefix.setPrefixLength(prefixLength);
			} catch (NumberFormatException nfe) {
				throw new BadRequestException(
						Util.concatenateParamsToLabel("#{error.invalid.dataType}", "prefix length", "Number"));
			}
		}
		try {
			// There's no "getLong" method
			JsonNumber number = object.getJsonNumber("asn");
			if (number != null) {
				slurmPrefix.setAsn(number.longValueExact());
			} else if (type == SlurmPrefix.TYPE_ASSERTION) {
				throw new BadRequestException("#{error.slurm.asnRequired}");
			} else if (slurmPrefix.getStartPrefix() == null) {
				// In a Filter is optional, but either a prefix or an asn must be present
				throw new BadRequestException("#{error.slurm.prefix.prefixOrAsnRequired}");
			}
		} catch (ClassCastException cce) {
			throw new BadRequestException(Util.concatenateParamsToLabel("#{error.invalid.dataType}", "asn", "Number"));
		} catch (ArithmeticException e) {
			throw new BadRequestException(Util.concatenateParamsToLabel("#{error.slurm.asnFormat}",
					ApiObject.ASN_MIN_VALUE, ApiObject.ASN_MAX_VALUE));
		}

		try {
			slurmPrefix.setPrefixMaxLength(object.getInt("maxPrefixLength"));
		} catch (NullPointerException npe) {
			// Optional in both cases, do nothing
		} catch (ClassCastException cce) {
			throw new BadRequestException(
					Util.concatenateParamsToLabel("#{error.invalid.dataType}", "maxPrefixLength", "Number"));
		}

		try {
			slurmPrefix.setComment(object.getString("comment"));
		} catch (NullPointerException npe) {
			// It's RECOMMENDED, so expect it as required in both cases
			throw new BadRequestException("#{error.slurm.commentRequired}");
		} catch (ClassCastException cce) {
			throw new BadRequestException(
					Util.concatenateParamsToLabel("#{error.invalid.dataType}", "comment", "String"));
		}

		return slurmPrefix;
	}
}

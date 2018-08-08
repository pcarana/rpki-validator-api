package mx.nic.lab.rpki.api.servlet.slurm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

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
import mx.nic.lab.rpki.api.result.slurm.SlurmBgpsecListResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmBgpsecSingleResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmCreateResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.api.util.CMSUtil;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.ValidationException;
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
			result = new SlurmBgpsecSingleResult(slurmBgpsec);
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
	private ApiResult handlePost(HttpServletRequest request, SlurmBgpsecDAO dao)
			throws HttpException, ApiDataAccessException {
		// Get the service and validate expected body
		List<String> additionalPathInfo = Util.getAdditionaPathInfo(request, 1, false);

		// Check if is a filter/assertion request
		int type;
		String requestedService = additionalPathInfo.get(0);
		if (requestedService.equals(FILTER_SERVICE)) {
			type = SlurmBgpsec.TYPE_FILTER;
		} else if (requestedService.equals(ASSERTION_SERVICE)) {
			type = SlurmBgpsec.TYPE_ASSERTION;
		} else {
			// Nothing to do, a NotFound Exception will be thrown
			return null;
		}
		SlurmBgpsec newSlurmBgpsec = getSlurmBgpsecFromBody(request, type);
		newSlurmBgpsec.setType(type);
		try {
			Long createdSlurmBgpsec = dao.create(newSlurmBgpsec);
			if (createdSlurmBgpsec == null) {
				throw new ConflictException("#{error.creationIncomplete}");
			}
			SlurmCreateResult result = new SlurmCreateResult(createdSlurmBgpsec);
			result.setCode(HttpServletResponse.SC_CREATED);
			return result;
		} catch (ApiDataAccessException e) {
			if (e instanceof ValidationException) {
				// Some errors must be mapped so that match to the clients request
				ValidationException ve = (ValidationException) e;
				if (ve.getValidationErrors() != null) {
					ve.getValidationErrors().forEach((error) -> {
						String field = error.getField();
						if (field == null) {
							return;
						}
						if (field.equals(SlurmBgpsec.SKI)) {
							error.setField("SKI");
							return;
						}
					});
				}
			}
			throw e;
		}
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
	private ApiResult handleDelete(HttpServletRequest request, SlurmBgpsecDAO dao)
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
	 * Get a {@link SlurmBgpsec} from the body request, runs some basic validations
	 * 
	 * @param request
	 * @param type
	 * @return
	 * @throws HttpException
	 */
	private SlurmBgpsec getSlurmBgpsecFromBody(HttpServletRequest request, int type) throws HttpException {
		SlurmBgpsec slurmBgpsec = new SlurmBgpsec();
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
			if (!key.matches("(asn|SKI|publicKey|comment)")) {
				invalidKeys.add(key);
			}
		}
		if (!invalidKeys.isEmpty()) {
			throw new BadRequestException(
					Util.concatenateParamsToLabel("#{error.invalid.keys}", invalidKeys.toString()));
		}

		try {
			// There's no "getLong" method
			JsonNumber number = object.getJsonNumber("asn");
			if (number != null) {
				slurmBgpsec.setAsn(number.longValueExact());
			} else if (type == SlurmBgpsec.TYPE_ASSERTION) {
				throw new BadRequestException("#{error.slurm.asnRequired}");
			}
		} catch (ClassCastException cce) {
			throw new BadRequestException(Util.concatenateParamsToLabel("#{error.invalid.dataType}", "asn", "Number"));
		}

		try {
			String value = object.getString("SKI");
			// If the value is sent, it can't be an empty value
			if (value.trim().isEmpty()) {
				throw new BadRequestException("#{error.slurm.bgpsec.skiEmpty}");
			}
			slurmBgpsec.setSki(value.trim());
		} catch (NullPointerException npe) {
			if (type == SlurmBgpsec.TYPE_ASSERTION) {
				throw new BadRequestException("#{error.slurm.bgpsec.skiRequired}");
			} else if (slurmBgpsec.getAsn() == null) {
				// In a Filter is optional, but either an asn or a SKI must be present
				throw new BadRequestException("#{error.slurm.bgpsec.asnOrSkiRequired}");
			}
		} catch (ClassCastException cce) {
			throw new BadRequestException(Util.concatenateParamsToLabel("#{error.invalid.dataType}", "SKI", "String"));
		}

		try {
			String value = object.getString("publicKey");
			if (value.trim().isEmpty()) {
				throw new BadRequestException("#{error.slurm.bgpsec.publicKeyEmpty}");
			}
			slurmBgpsec.setPublicKey(value.trim());
		} catch (NullPointerException npe) {
			if (type == SlurmBgpsec.TYPE_ASSERTION) {
				throw new BadRequestException("#{error.slurm.bgpsec.publicKeyRequired}");
			}
		} catch (ClassCastException cce) {
			throw new BadRequestException(
					Util.concatenateParamsToLabel("#{error.invalid.dataType}", "publicKey", "String"));
		}

		try {
			String value = object.getString("comment");
			if (value.trim().isEmpty()) {
				throw new BadRequestException("#{error.slurm.commentEmpty}");
			}
			slurmBgpsec.setComment(value.trim());
		} catch (NullPointerException npe) {
			// It's RECOMMENDED, so expect it as required in both cases
			throw new BadRequestException("#{error.slurm.commentRequired}");
		} catch (ClassCastException cce) {
			throw new BadRequestException(
					Util.concatenateParamsToLabel("#{error.invalid.dataType}", "comment", "String"));
		}

		// Check SKI and publicKey are sent base64url encoded, and verify its value
		if (slurmBgpsec.getSki() != null && !slurmBgpsec.getSki().trim().isEmpty()) {
			try {
				byte[] decodedSki = Base64.getUrlDecoder().decode(slurmBgpsec.getSki());
				if (!CMSUtil.isValidSubjectKeyIdentifier(decodedSki)) {
					throw new BadRequestException("#{error.slurm.bgpsec.skiInvalid}");
				}
			} catch (IllegalArgumentException e) {
				throw new BadRequestException(Util.concatenateParamsToLabel("#{error.slurm.bgpsec.notBase64}", "SKI"));
			}
		}

		if (slurmBgpsec.getPublicKey() != null && !slurmBgpsec.getPublicKey().trim().isEmpty()) {
			try {
				byte[] decodedPk = Base64.getUrlDecoder().decode(slurmBgpsec.getPublicKey());
				if (!CMSUtil.isValidSubjectPublicKey(decodedPk)) {
					throw new BadRequestException("#{error.slurm.bgpsec.publicKeyInvalid}");
				}
			} catch (IllegalArgumentException e) {
				throw new BadRequestException(
						Util.concatenateParamsToLabel("#{error.slurm.bgpsec.notBase64}", "publicKey"));
			}
		}
		return slurmBgpsec;
	}
}

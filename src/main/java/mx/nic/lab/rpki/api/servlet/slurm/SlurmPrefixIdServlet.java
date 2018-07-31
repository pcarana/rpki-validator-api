package mx.nic.lab.rpki.api.servlet.slurm;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.EmptyResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmPrefixListResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmPrefixResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.http.BadRequestException;
import mx.nic.lab.rpki.db.exception.http.HttpException;
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
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, SlurmPrefixDAO dao)
			throws ApiDataAccessException {
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
	 * @throws ApiDataAccessException
	 */
	private ApiResult handleGet(HttpServletRequest request, SlurmPrefixDAO dao) throws ApiDataAccessException {
		// The GET request only expects 3 possible paths: {id}, "filter", or "assertion"
		List<String> additionalPathInfo = Util.getAdditionaPathInfo(request, 1, false);
		String requestedService = additionalPathInfo.get(0);
		ApiResult result = null;

		// Check if is a filter/assertion request
		if (requestedService.equals(FILTER_SERVICE)) {
			List<SlurmPrefix> filters = dao.getAllByType(SlurmPrefix.TYPE_FILTER);
			result = new SlurmPrefixListResult(filters);
		} else if (requestedService.equals(ASSERTION_SERVICE)) {
			List<SlurmPrefix> assertions = dao.getAllByType(SlurmPrefix.TYPE_ASSERTION);
			result = new SlurmPrefixListResult(assertions);
		} else {
			// Check if is an ID
			Long id = null;
			try {
				id = Long.parseLong(requestedService);
			} catch (NumberFormatException e) {
				throw new BadRequestException("#{exception.invalidId}", e);
			}
			SlurmPrefix slurmPrefix = dao.getById(id);
			if (slurmPrefix == null) {
				return null;
			}
			result = new SlurmPrefixResult(slurmPrefix);
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
	private ApiResult handlePost(HttpServletRequest request, SlurmPrefixDAO dao) throws ApiDataAccessException {
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
		SlurmPrefix createdSlurmPrefix = dao.create(newSlurmPrefix);
		return new SlurmPrefixResult(createdSlurmPrefix);
	}

	/**
	 * Handle a DELETE request, 1 parameter is expected: the ID of the prefix to
	 * delete. A single result will be returned depending on the result of the
	 * delete operation.
	 * 
	 * @param request
	 * @param dao
	 * @return
	 * @throws ApiDataAccessException
	 */
	private ApiResult handleDelete(HttpServletRequest request, SlurmPrefixDAO dao) throws ApiDataAccessException {
		List<String> additionalPathInfo = Util.getAdditionaPathInfo(request, 1, false);
		// First check that the object exists
		Long id = null;
		try {
			id = Long.parseLong(additionalPathInfo.get(0));
		} catch (NumberFormatException e) {
			throw new BadRequestException("#{exception.invalidId}", e);
		}
		if (!dao.deleteById(id)) {
			throw new HttpException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "#{exception.unavailableService}");
		}
		return new EmptyResult();
	}

	private SlurmPrefix getSlurmPrefixFromBody(HttpServletRequest request, int type) throws ApiDataAccessException {
		SlurmPrefix slurmPrefix = new SlurmPrefix();
		JsonObject object = null;
		try (JsonReader reader = Json.createReader(request.getReader())) {
			object = reader.readObject();
		} catch (IOException e) {
			throw new HttpException(500, "Error getting reader");
		}
		// Check for extra keys (invalid keys)
		List<String> invalidKeys = new ArrayList<>();
		for (String key : object.keySet()) {
			if (!key.matches("(prefix|asn|maxPrefixLength|comment)")) {
				invalidKeys.add(key);
			}
		}
		if (!invalidKeys.isEmpty()) {
			throw new BadRequestException("Invalid keys found: " + invalidKeys.toString());
		}
		String prefixRcv = null;
		try {
			prefixRcv = object.getString("prefix");
		} catch (NullPointerException npe) {
			if (type == SlurmPrefix.TYPE_ASSERTION) {
				throw new BadRequestException("A prefix is needed");
			}
		} catch (ClassCastException cce) {
			throw new BadRequestException("Invalid prefix data type, String expected");
		}
		if (prefixRcv != null) {
			String[] prefixArr = prefixRcv.split("/");
			if (prefixArr.length != 2) {
				throw new BadRequestException("Invalid prefix, must be in format <prefix>/<prefix_length");
			}

			try {
				InetAddress prefixAddress = InetAddress.getByName(prefixArr[0]);
				slurmPrefix.setStartPrefix(prefixAddress.getAddress());
				slurmPrefix.setPrefixText(prefixAddress.getHostAddress());
			} catch (UnknownHostException e) {
				throw new BadRequestException("Invalid prefix");
			}
			try {
				int prefixLength = Integer.valueOf(prefixArr[1]);
				slurmPrefix.setPrefixLength(prefixLength);
			} catch (NumberFormatException nfe) {
				throw new BadRequestException("Invalid prefix length");
			}
		}
		try {
			// There's no "getLong" method
			JsonNumber number = object.getJsonNumber("asn");
			if (number != null) {
				slurmPrefix.setAsn(number.longValueExact());
			} else if (type == SlurmPrefix.TYPE_ASSERTION) {
				throw new BadRequestException("An ASN is needed");
			}
		} catch (ClassCastException cce) {
			throw new BadRequestException("Invalid ASN");
		}

		try {
			slurmPrefix.setPrefixMaxLength(object.getInt("maxPrefixLength"));
		} catch (NullPointerException npe) {
			// Optional in both cases, do nothing
		} catch (ClassCastException cce) {
			throw new BadRequestException("Invalid max prefix length data type, Integer expected");
		}

		try {
			slurmPrefix.setComment(object.getString("comment"));
		} catch (NullPointerException npe) {
			// It's RECOMMENDED, so expect it as required in both cases
			throw new BadRequestException("Comment expected");
		} catch (ClassCastException cce) {
			throw new BadRequestException("Invalid comment data type, String expected");
		}

		// Calculate End prefix (if applies, only when there's a Prefix Max Length)
		if (slurmPrefix.getStartPrefix() != null && slurmPrefix.getPrefixLength() != null
				&& slurmPrefix.getPrefixMaxLength() != null) {
			byte[] endPrefix = slurmPrefix.getStartPrefix().clone();
			int prefixLength = slurmPrefix.getPrefixLength();
			int maxPrefixLength = slurmPrefix.getPrefixMaxLength();
			int bytesBase = prefixLength / 8;
			int bitsBase = prefixLength % 8;
			int bytesMask = maxPrefixLength / 8;
			int bitsMask = maxPrefixLength % 8;
			if (maxPrefixLength > prefixLength && bytesBase < endPrefix.length) {
				int currByte = bytesBase;
				if (bytesMask > bytesBase) {
					endPrefix[currByte] |= (255 >> bitsBase);
					currByte++;
					for (; currByte < bytesMask; currByte++) {
						endPrefix[currByte] |= 255;
					}
					bitsBase = 0;
				}
				if (currByte < endPrefix.length) {
					endPrefix[currByte] |= ((byte) (255 << (8 - bitsMask)) & (255 >> bitsBase));
				}
			}
			slurmPrefix.setEndPrefix(endPrefix);
		} else {
			slurmPrefix.setEndPrefix(slurmPrefix.getStartPrefix());
		}

		return slurmPrefix;
	}
}

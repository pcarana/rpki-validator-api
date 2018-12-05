package mx.nic.lab.rpki.api.servlet.validate;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.BadRequestException;
import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.validate.RouteValidationResult;
import mx.nic.lab.rpki.api.servlet.DataAccessServlet;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.ApiObject;
import mx.nic.lab.rpki.db.pojo.RouteValidation;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.RouteValidationDAO;

/**
 * Servlet to simulate a route validation
 *
 */
@WebServlet(name = "routeValidation", value = { "/validate/*" })
public class RouteValidationServlet extends DataAccessServlet<RouteValidationDAO> {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Optional query parameter to indicate if a full check must be done, if
	 * <code>false</code> then the search will be only for the exact match
	 */
	private static final String PARAM_FULL_CHECK = "fullCheck";

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, RouteValidationDAO dao)
			throws HttpException, ApiDataAccessException {
		// Exactly 3 parameters are expected at the URI, it must be a: long(asn), text
		// (prefix), and integer (prefix length)
		List<String> additionalPathInfo = Util.getAdditionaPathInfo(request, 3, false);
		if (additionalPathInfo.size() != 3) {
			throw new BadRequestException("#{error.missingArguments}");
		}
		// Only the exact match ("true") will be treated as such
		String fullCheckStr = request.getParameter(PARAM_FULL_CHECK);
		if (fullCheckStr != null && !fullCheckStr.trim().equals("true") && !fullCheckStr.trim().equals("false")) {
			throw new BadRequestException(
					Util.concatenateParamsToLabel("#{error.invalidParameter}", PARAM_FULL_CHECK, "true, false"));
		}
		boolean fullCheck = fullCheckStr == null ? false : Boolean.parseBoolean(fullCheckStr.trim());
		// Basic validations
		Long asn = null;
		try {
			asn = Long.parseLong(additionalPathInfo.get(0));
			if (asn < ApiObject.ASN_MIN_VALUE || asn > ApiObject.ASN_MAX_VALUE) {
				throw new BadRequestException("#{error.route.validation.asn.outOfRange}");
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException("#{error.route.validation.asn.invalid}", e);
		}
		InetAddress prefix = null;
		try {
			prefix = InetAddress.getByName(additionalPathInfo.get(1));
		} catch (UnknownHostException e) {
			throw new BadRequestException("#{error.route.validation.prefix.notIp}", e);
		}
		Integer prefixLength = null;
		try {
			prefixLength = Integer.parseInt(additionalPathInfo.get(2));
			int maxLength = prefix instanceof Inet4Address ? 32 : 128;
			if (prefixLength < 1 || prefixLength > maxLength) {
				throw new BadRequestException("#{error.route.validation.prefixLength.outOfRange}");
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException("#{error.route.validation.prefixLength.invalid}", e);
		}
		if (!isValidPrefix(prefix.getAddress(), prefixLength)) {
			throw new BadRequestException("#{error.route.validation.prefix.invalid}");
		}

		RouteValidation routeValidation = dao.validate(asn, prefix.getAddress(), prefixLength, fullCheck);
		return new RouteValidationResult(routeValidation);
	}

	/**
	 * Check if the prefix is effectively a prefix and not just an IP address
	 * 
	 * @param prefix
	 * @param prefixLength
	 * @return
	 */
	private boolean isValidPrefix(byte[] prefix, Integer prefixLength) {
		int bytesBase = prefixLength / 8;
		int bitsBase = prefixLength % 8;
		byte[] prefixLengthMask = new byte[prefix.length];
		int currByte = 0;
		for (; currByte < bytesBase; currByte++) {
			prefixLengthMask[currByte] |= 255;
		}
		if (currByte < prefixLengthMask.length) {
			prefixLengthMask[currByte] = (byte) (255 << (8 - bitsBase));
		}
		BigInteger ip = new BigInteger(prefix);
		BigInteger mask = new BigInteger(prefixLengthMask);
		return ip.or(mask).equals(mask);
	}

	@Override
	protected String getServedObjectName() {
		return "routeValidation";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.GET);
	}

	@Override
	protected RouteValidationDAO initAccessDAO() throws ApiDataAccessException {
		return DataAccessService.getRouteValidationDAO();
	}

	@Override
	protected Map<String, String> getValidSortKeys(HttpServletRequest request) {
		return null;
	}

}

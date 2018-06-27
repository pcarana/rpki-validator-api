package mx.nic.lab.rpki.api.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.db.exception.http.BadRequestException;
import mx.nic.lab.rpki.db.exception.http.HttpException;
import mx.nic.lab.rpki.db.exception.http.NotFoundException;

/**
 * Utilery class
 *
 */
public class Util {

	/**
	 * Return ther request parameters as a String array. If the request's URI is
	 * /server/ip/192.0.2.0/24, then this returns ["192.0.2.0", "24"].
	 * 
	 * @param request
	 *            request you want the arguments from.
	 * @param maxParamsExpected
	 *            maximum number of parameters expected, negative value means
	 *            indefinite
	 * @return request arguments.
	 * @throws HttpException
	 *             <code>request</code> is not a valid URI.
	 */
	public static String[] getRequestParams(HttpServletRequest request, int maxParamsExpected) throws HttpException {
		try {
			URLDecoder.decode(request.getRequestURI(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new BadRequestException("The request does not appear to be UTF-8 encoded.", e);
		}

		String pathInfo = request.getPathInfo();
		if (pathInfo == null || pathInfo.equals("/")) {
			throw new NotFoundException(
					"The request does not appear to be a valid URI. I might need more arguments than that.");
		}
		// Ignores the first "/"
		String[] requestParams = pathInfo.substring(1).split("/");
		// If maxParamsExpected is sent then validate against its value
		if (maxParamsExpected >= 0 && requestParams.length > maxParamsExpected) {
			throw new NotFoundException(request.getRequestURI());
		}
		return requestParams;
	}
}

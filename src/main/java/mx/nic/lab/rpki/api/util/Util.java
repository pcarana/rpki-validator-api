package mx.nic.lab.rpki.api.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
	 * Return the additional path info of a request URI as a String List, i.e. If
	 * the request's URI is "/server/ip/192.0.2.0/24", and the servlet path where
	 * this request was received is "/server/ip/*", then this returns the list
	 * ["192.0.2.0", "24"].
	 * 
	 * @param request
	 *            request you want the arguments from.
	 * @param maxParamsExpected
	 *            maximum number of parameters expected, negative value means
	 *            indefinite
	 * @param allowEmptyPath
	 *            boolean to prove if an empty path is allowed
	 * @return <code>List</code> with additional path info.
	 * @throws HttpException
	 *             <code>request</code> is not a valid URI.
	 */
	public static List<String> getAdditionaPathInfo(HttpServletRequest request, int maxParamsExpected,
			boolean allowEmptyPath) throws HttpException {
		String pathInfo = request.getPathInfo();
		if (pathInfo == null || pathInfo.equals("/")) {
			if (!allowEmptyPath) {
				throw new BadRequestException("#{exception.missingArguments}");
			}
			return Collections.emptyList();
		}
		// Ignores the first "/"
		String[] stringArr = pathInfo.substring(1).split("/");
		List<String> requestParams = Arrays.asList(stringArr);
		// If maxParamsExpected is sent then validate against its value
		if (maxParamsExpected >= 0 && requestParams.size() > maxParamsExpected) {
			throw new NotFoundException(request.getRequestURI());
		}
		return requestParams;
	}
}

package mx.nic.lab.rpki.api.util;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.BadRequestException;
import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.exception.NotFoundException;

/**
 * Utilery class
 *
 */
public class Util {

	/**
	 * Common format to return dates as String
	 */
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HHmmss.SSS'Z'";

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
				throw new BadRequestException("#{error.missingArguments}");
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
		// Check empty strings
		for (String param : requestParams) {
			if (param.isEmpty()) {
				throw new BadRequestException("#{error.missingArguments}");
			}
		}
		return requestParams;
	}

	/**
	 * Concatenates the parameter values expected at the indicated label, the final
	 * result is the String: <code>label{param1}{param2}...{paramN}</code><br>
	 * Later this should be used to replace the parameters with its corresponding
	 * value.
	 * 
	 * @param label
	 *            a label ID (it must exist in a bundle)
	 * @param params
	 *            the parameters to concatenate
	 * @return The label ID with the parameters concatenated
	 */
	public static String concatenateParamsToLabel(String label, Object... params) {
		StringBuilder sb = new StringBuilder();
		sb.append(label);
		if (params.length > 0) {
			for (Object param : params) {
				sb.append("{");
				sb.append(param);
				sb.append("}");
			}
		}
		return sb.toString();
	}

	/**
	 * Get the date as a formatted String
	 * 
	 * @param date
	 * @return
	 */
	public static String getFormattedDate(Date date) {
		SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
		return df.format(date);
	}
}

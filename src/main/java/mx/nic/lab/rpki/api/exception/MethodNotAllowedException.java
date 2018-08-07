package mx.nic.lab.rpki.api.exception;

import javax.servlet.http.HttpServletResponse;;

/**
 * An HTTP 405 error.
 * 
 * "A request method is not supported for the requested resource; for example, a
 * GET request on a form that requires data to be presented via POST, or a PUT
 * request on a read-only resource."
 * 
 * (Quoted from Wikipedia.)
 */
public class MethodNotAllowedException extends HttpException {

	private static final long serialVersionUID = 1L;
	private static final int CODE = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
	private static final String DEFAULT_MSG = "#{error.methodNotAllowed}";

	public MethodNotAllowedException() {
		super(CODE, DEFAULT_MSG);
	}

	public MethodNotAllowedException(String message) {
		super(CODE, message);
	}

	public MethodNotAllowedException(Throwable cause) {
		super(CODE, DEFAULT_MSG, cause);
	}

	public MethodNotAllowedException(String message, Throwable cause) {
		super(CODE, message, cause);
	}

}

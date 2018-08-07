package mx.nic.lab.rpki.api.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * Handles an internal server error gracefully (HTTP 500 error).
 * <p>
 * "A generic error message, given when an unexpected condition was encountered
 * and no more specific message is suitable."
 * <p>
 * (Quoted from Wikipedia.)
 */
public class InternalServerErrorException extends HttpException {

	private static final long serialVersionUID = 1L;
	private static final int CODE = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	private static final String DEFAULT_MSG = "#{error.internalError}";

	public InternalServerErrorException() {
		super(CODE, DEFAULT_MSG);
	}

	public InternalServerErrorException(String message) {
		super(CODE, message);
	}

	public InternalServerErrorException(Throwable cause) {
		super(CODE, DEFAULT_MSG, cause);
	}

	public InternalServerErrorException(String message, Throwable cause) {
		super(CODE, message, cause);
	}

}

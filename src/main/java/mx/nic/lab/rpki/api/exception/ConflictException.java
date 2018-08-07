package mx.nic.lab.rpki.api.exception;

import javax.servlet.http.HttpServletResponse;

/**
 * The requested action couldn't be performed, the client should try again (HTTP
 * 409 error).
 * <p>
 * "Indicates that the request could not be processed because of conflict in the
 * current state of the resource, such as an edit conflict between multiple
 * simultaneous updates."
 * <p>
 * (Quoted from Wikipedia.)
 */
public class ConflictException extends HttpException {

	private static final long serialVersionUID = 1L;
	private static final int CODE = HttpServletResponse.SC_CONFLICT;
	private static final String DEFAULT_MSG = "#{error.conflict}";

	public ConflictException() {
		super(CODE, DEFAULT_MSG);
	}

	public ConflictException(String message) {
		super(CODE, message);
	}

	public ConflictException(Throwable cause) {
		super(CODE, DEFAULT_MSG, cause);
	}

	public ConflictException(String message, Throwable cause) {
		super(CODE, message, cause);
	}

}

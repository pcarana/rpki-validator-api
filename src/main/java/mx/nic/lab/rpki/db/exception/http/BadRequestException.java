package mx.nic.lab.rpki.db.exception.http;

import javax.servlet.http.HttpServletResponse;

import mx.nic.lab.rpki.db.exception.IpAddressFormatException;;

/**
 * An HTTP 400 error.
 * 
 * "The server cannot or will not process the request due to an apparent client
 * error (e.g., malformed request syntax, too large size, invalid request
 * message framing, or deceptive request routing)."
 * 
 * (Quoted from Wikipedia.)
 */
public class BadRequestException extends HttpException {

	private static final long serialVersionUID = 1L;
	private static final int CODE = HttpServletResponse.SC_BAD_REQUEST;
	private static final String DEFAULT_MSG = "Bad Request";

	public BadRequestException() {
		super(CODE, DEFAULT_MSG);
	}

	public BadRequestException(String message) {
		super(CODE, message);
	}

	public BadRequestException(Throwable cause) {
		super(CODE, DEFAULT_MSG, cause);
	}

	public BadRequestException(IpAddressFormatException e) {
		super(CODE, e.getMessage(), e);
	}

	public BadRequestException(String message, Throwable cause) {
		super(CODE, message, cause);
	}

}

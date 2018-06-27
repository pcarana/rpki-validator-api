package mx.nic.lab.rpki.db.exception;

/**
 * A checked exception to throw when there's a problem initializing something.
 */
public class InitializationException extends ApiDataAccessException {

	private static final long serialVersionUID = 1L;

	public InitializationException(String message) {
		super(message);
	}

	public InitializationException(String msg, Throwable cause) {
		super(msg, cause);
	}

}

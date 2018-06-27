package mx.nic.lab.rpki.db.exception;

/**
 * A checked exception when there's a problem accessing data. Implementations
 * are free to extend from this.
 */
public class ApiDataAccessException extends Exception {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = -4882901072367223361L;

	public ApiDataAccessException() {
		super();
	}

	public ApiDataAccessException(String message) {
		super(message);
	}

	public ApiDataAccessException(Throwable cause) {
		super(cause);
	}

	public ApiDataAccessException(String message, Throwable cause) {
		super(message, cause);
	}

}

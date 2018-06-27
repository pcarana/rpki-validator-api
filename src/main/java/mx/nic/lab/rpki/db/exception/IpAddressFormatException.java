package mx.nic.lab.rpki.db.exception;

/**
 * The input cannot be converted to an IP address (or a prefix) due to syntactic
 * or semantic problems.
 */
public class IpAddressFormatException extends Exception {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	public IpAddressFormatException(String msg) {
		super(msg);
	}

	public IpAddressFormatException(String msg, Throwable cause) {
		super(msg, cause);
	}

}

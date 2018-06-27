package mx.nic.lab.rpki.db.exception.http;

import mx.nic.lab.rpki.db.exception.ApiDataAccessException;

/**
 * An exception intended to become an HTTP error code.
 * <p>
 * So I'm aware that {@link javax.xml.ws.http.HTTPException} exists, but I find
 * it rather unusable. It is located in a weird package,
 * {@link javax.servlet.http.HttpServlet} does not handle it gracefully, its
 * name's casing is not consistent with <code>javax.servlet.http</code>'s
 * classes, I can't find much usage of it in the web, it cannot carry an error
 * message, and it's unchecked for some baffling reason. So it's kind of a
 * trainwreck, so I decided to make my own.
 */
public class HttpException extends ApiDataAccessException {

	private static final long serialVersionUID = 1L;

	/**
	 * The code we'll return in the HTTP response.
	 */
	private int httpResponseStatusCode;

	public HttpException(int httpResponseStatusCode, String message) {
		super(message);
		this.httpResponseStatusCode = httpResponseStatusCode;
	}

	public HttpException(int httpResponseStatusCode, String message, Throwable cause) {
		super(message, cause);
		this.httpResponseStatusCode = httpResponseStatusCode;
	}

	/**
	 * @see #httpResponseStatusCode
	 */
	public int getHttpResponseStatusCode() {
		return httpResponseStatusCode;
	}

}

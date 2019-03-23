package mx.nic.lab.rpki.api.result;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

public abstract class ApiResultAbstract {
	/**
	 * HTTP response code if a servlet wishes to customize it
	 */
	protected int code;

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}


	/**
	 * Writes in Http response, rendering the result in the desired format
	 * @param locale locale of the request
	 * @param resp http response to write in
	 * @throws IOException
	 *
	 */
	public abstract void printBody(Locale locale, HttpServletResponse resp) throws IOException;
}

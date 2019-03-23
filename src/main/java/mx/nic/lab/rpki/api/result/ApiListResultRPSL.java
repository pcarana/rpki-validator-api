package mx.nic.lab.rpki.api.result;

import mx.nic.lab.rpki.db.pojo.ApiObject;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

/**
 * Result corresponding to a list to be rendered as the result of an API request.
 * Its type <code>T</code> extends from an {@link ApiObject}.
 * This class extends from {@link ApiResultAbstract}.
 * It implements printBody in order to represent the list in a RPSL format.
 *
 * @param <T> Type of the {@link ApiObject} list elements to be represented in RPSL format
 */
public abstract class ApiListResultRPSL<T extends ApiObject> extends ApiResultAbstract {

	public static final String TYPE_RPSL = "text/plain;format=rpsl";

	private ListResult<T> listResult;
	private PagingParameters pagingParameters;


	@Override
	public void printBody(Locale locale, HttpServletResponse resp) throws IOException {
		// Render RESULT
		resp.setStatus(this.getCode());
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setContentType(TYPE_RPSL);
		try (Writer writer = resp.getWriter()) {
			for (T t : getListResult().getResults()) {
				writer.write(getRPSLString(t));
			}
		}
	}

	/**
	 * Converts an object to a string in RPSL format. Helps  printBody to render the response of the list.
	 * @param t object to be represented in RPSL format
	 * @return A RPSL string that represents t to help rendering the list.
	 */
	protected abstract String getRPSLString(T t);


	public ListResult<T> getListResult() {
		return listResult;
	}

	public void setListResult(ListResult<T> listResult) {
		this.listResult = listResult;
	}

	public PagingParameters getPagingParameters() {
		return pagingParameters;
	}

	public void setPagingParameters(PagingParameters pagingParameters) {
		this.pagingParameters = pagingParameters;
	}

}

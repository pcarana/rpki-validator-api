package mx.nic.lab.rpki.api.result;

import au.com.bytecode.opencsv.CSVWriter;
import mx.nic.lab.rpki.db.pojo.ApiObject;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

/**
 * Result corresponding to a list to be rendered as the result of an API request.
 * Its type <code>T</code> extends from an {@link ApiObject}.
 * This class extends from {@link ApiResultAbstract}.
 * It implements printBody in order to represent the list in a CSV format.
 *
 * @param <T> Type of the {@link ApiObject} list elements to be represented in CSV format
 */
public abstract class ApiListResultCSV<T extends ApiObject> extends ApiResultAbstract {

	public static final String TYPE_CSV = "text/csv";
	private ListResult<T> listResult;
	private PagingParameters pagingParameters;

	@Override
	public void printBody(Locale locale, HttpServletResponse resp) throws IOException {
		// Render RESULT
		resp.setStatus(this.getCode());
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setContentType(TYPE_CSV);
		try (CSVWriter writer = new CSVWriter(resp.getWriter())) {
			writer.writeNext(getTilte());
			getListResult().getResults().forEach(t ->  writer.writeNext(getLine(t)));
		}
	}

	/**
	 * Abstract method used by printBody to write the heading row of the csv output
	 * @return the heading row of the csv output
	 */
	protected abstract String[] getTilte();

	/**
	 * Abstract method used by printBody to write a line of the csv output
	 * @param t object to be converted
	 * @return the corresponding line in csv format
	 */
	protected abstract String[] getLine(T t);


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

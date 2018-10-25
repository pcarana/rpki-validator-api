package mx.nic.lab.rpki.api.result;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.db.pojo.ApiObject;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;

/**
 * Result corresponding to a {@link ListResult}, where its type <code>T</code>
 * extends from an {@link ApiObject}; this class extends from {@link ApiResult}.
 * The class returns a JSON structure with the following format:
 * 
 * <pre>
 * {
 *   results: [JSON Array - list of found results],
 *   found: [integer - number of total results found],
 *   returned: [integer - number of results returned after the paging is applied],
 *   page: {
 *     limit: [integer - paging limit applied to the search],
 *     offset: [integer - paging offset applied to the search]
 *   }
 * }
 * </pre>
 *
 * @param <T>
 *            Type of the {@link ApiObject} used at the {@link ListResult}
 */
public abstract class ApiListResult<T extends ApiObject> extends ApiResult {

	private ListResult<T> listResult;
	private PagingParameters pagingParameters;

	@Override
	public JsonStructure toJsonStructure() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("results", resultToJsonStructure());
		builder.add("found", listResult.getTotalFound());
		builder.add("returned", listResult.getResults().size());
		JsonObjectBuilder pageBuilder = Json.createObjectBuilder();
		pageBuilder.add("limit", pagingParameters.getLimit());
		if (pagingParameters.getOffset() > -1) {
			pageBuilder.add("offset", pagingParameters.getOffset());
		}
		builder.add("page", pageBuilder);
		return builder.build();
	}

	public abstract JsonStructure resultToJsonStructure();

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

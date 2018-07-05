package mx.nic.lab.rpki.api.result;

import java.util.List;

import mx.nic.lab.rpki.db.pojo.ApiObject;

/**
 * Result corresponding to a list of {@link ApiObject}, extends from
 * {@link ApiResult}
 *
 * @param <T>
 *            Type of the {@link ApiObject} list
 */
public abstract class ApiListResult<T> extends ApiResult {

	private List<? extends T> apiObjects;

	public List<? extends T> getApiObjects() {
		return apiObjects;
	}

	public void setApiObjects(List<? extends T> apiObjects) {
		this.apiObjects = apiObjects;
	}

}

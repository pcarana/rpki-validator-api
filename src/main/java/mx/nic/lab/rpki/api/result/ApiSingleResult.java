package mx.nic.lab.rpki.api.result;

import mx.nic.lab.rpki.db.pojo.ApiObject;

/**
 * Result corresponding to a single {@link ApiObject}, extends from
 * {@link ApiResult}
 *
 * @param <T>
 *            Type of the {@link ApiObject}
 */
public abstract class ApiSingleResult<T> extends ApiResult {

	private T apiObject;

	public T getApiObject() {
		return apiObject;
	}

	public void setApiObject(T apiObject) {
		this.apiObject = apiObject;
	}

}

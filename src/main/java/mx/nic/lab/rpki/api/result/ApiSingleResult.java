package mx.nic.lab.rpki.api.result;

import mx.nic.lab.rpki.db.pojo.ApiObject;

/**
 * Result corresponding to a single {@link ApiObject}, extends from
 * {@link ApiResult}
 *
 */
public abstract class ApiSingleResult extends ApiResult {

	private ApiObject apiObject;

	public ApiObject getApiObject() {
		return apiObject;
	}

	public void setApiObject(ApiObject apiObject) {
		this.apiObject = apiObject;
	}

	@Override
	public String toJson() {
		if (getApiObject() != null) {
			return getApiObject().toJson();
		}
		return "{}";
	}

}

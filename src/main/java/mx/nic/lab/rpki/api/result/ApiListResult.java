package mx.nic.lab.rpki.api.result;

import java.util.List;

import mx.nic.lab.rpki.db.pojo.ApiObject;

/**
 * Result corresponding to a list of {@link ApiObject}, extends from
 * {@link ApiResult}
 *
 */
public abstract class ApiListResult extends ApiResult {

	private List<? extends ApiObject> apiObjects;

	public List<? extends ApiObject> getApiObjects() {
		return apiObjects;
	}

	public void setApiObjects(List<? extends ApiObject> apiObjects) {
		this.apiObjects = apiObjects;
	}

	@Override
	public String toJson() {
		if (apiObjects != null) {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			apiObjects.forEach(obj -> sb.append(obj.toJson()).append(","));
			sb.deleteCharAt(sb.length() - 1);
			sb.append("]");
			return sb.toString();
		}
		return "[]";
	}

}

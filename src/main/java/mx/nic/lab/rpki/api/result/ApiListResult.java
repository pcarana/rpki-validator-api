package mx.nic.lab.rpki.api.result;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

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
	public String toJsonString() {
		if (apiObjects == null) {
			return JsonObject.EMPTY_JSON_ARRAY.toString();
		}
		JsonArrayBuilder jsonBuilder = Json.createArrayBuilder();
		apiObjects.forEach(obj -> jsonBuilder.add(obj.toJsonObject()));
		return jsonBuilder.build().toString();
	}

}

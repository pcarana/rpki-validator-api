package mx.nic.lab.rpki.api.result;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.db.pojo.Roa;

/**
 * Result that represents a list of Roas
 *
 */
public class RoaListResult extends ApiListResult<Roa> {

	public RoaListResult(List<Roa> roas) {
		super();
		setApiObjects(roas);
	}

	@Override
	public JsonStructure toJsonStructure() {
		if (getApiObjects() == null || getApiObjects().isEmpty()) {
			return JsonObject.EMPTY_JSON_ARRAY;
		}
		JsonArrayBuilder jsonBuilder = Json.createArrayBuilder();
		// Use the RoaResult implementation
		getApiObjects().forEach(obj -> {
			RoaResult temp = new RoaResult(obj);
			jsonBuilder.add(temp.toJsonStructure());
		});
		return jsonBuilder.build();
	}

}

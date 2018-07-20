package mx.nic.lab.rpki.api.result.tal;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiListResult;
import mx.nic.lab.rpki.db.pojo.Tal;

/**
 * Result that represents a list of Tals
 *
 */
public class TalListResult extends ApiListResult<Tal> {

	public TalListResult(List<Tal> tals) {
		super();
		setApiObjects(tals);
	}

	@Override
	public JsonStructure toJsonStructure() {
		if (getApiObjects() == null) {
			return JsonObject.EMPTY_JSON_ARRAY;
		}
		JsonArrayBuilder jsonBuilder = Json.createArrayBuilder();
		// Use the TalResult implementation
		getApiObjects().forEach(obj -> {
			TalResult temp = new TalResult(obj);
			jsonBuilder.add(temp.toJsonStructure());
		});
		return jsonBuilder.build();
	}

}

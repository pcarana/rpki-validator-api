package mx.nic.lab.rpki.api.result.tal;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiListResult;
import mx.nic.lab.rpki.db.pojo.Tal;

/**
 * Result that represents a list of Tals that will be synchronized
 *
 */
public class TalSyncAllResult extends ApiListResult<Tal> {

	public TalSyncAllResult(List<Tal> tals) {
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
			TalSyncOneResult temp = new TalSyncOneResult(obj);
			jsonBuilder.add(temp.toJsonStructure());
		});
		return jsonBuilder.build();
	}

}

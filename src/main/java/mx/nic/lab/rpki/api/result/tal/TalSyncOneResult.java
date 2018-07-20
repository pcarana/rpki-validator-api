package mx.nic.lab.rpki.api.result.tal;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
import mx.nic.lab.rpki.db.pojo.Tal;

/**
 * Result that represents a single Tal that will be synchronized
 *
 */
public class TalSyncOneResult extends ApiSingleResult<Tal> {

	public TalSyncOneResult(Tal tal) {
		super();
		setApiObject(tal);
	}

	@Override
	public JsonStructure toJsonStructure() {
		Tal tal = getApiObject();
		if (tal == null) {
			return JsonObject.EMPTY_JSON_OBJECT;
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if (tal.getId() != null) {
			builder.add("id", tal.getId());
		} else {
			builder.addNull("id");
		}
		if (tal.getLastSync() != null) {
			builder.add("lastSync", tal.getLastSync());
		} else {
			builder.addNull("lastSync");
		}
		if (tal.getStatus() != null) {
			builder.add("status", tal.getStatus());
		} else {
			builder.addNull("status");
		}
		if (tal.getName() != null) {
			builder.add("name", tal.getName());
		} else {
			builder.addNull("name");
		}

		return builder.build();
	}
}

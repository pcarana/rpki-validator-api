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
		addKeyValueToBuilder(builder, "id", tal.getId(), true);
		addKeyValueToBuilder(builder, "lastSync", tal.getLastSync(), true);
		addKeyValueToBuilder(builder, "status", tal.getStatus(), true);
		addKeyValueToBuilder(builder, "name", tal.getName(), true);

		return builder.build();
	}
}

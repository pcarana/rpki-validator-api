package mx.nic.lab.rpki.api.result.tal;

import javax.json.Json;
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
			return null;
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "id", tal.getId(), true);
		addKeyValueToBuilder(builder, "lastSync", tal.getLastSync(), true);
		addKeyValueToBuilder(builder, "syncStatus", tal.getSyncStatus(), true);
		addKeyValueToBuilder(builder, "name", tal.getName(), true);

		return builder.build();
	}
}

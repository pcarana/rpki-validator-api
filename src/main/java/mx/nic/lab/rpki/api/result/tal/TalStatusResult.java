package mx.nic.lab.rpki.api.result.tal;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
import mx.nic.lab.rpki.db.pojo.Tal;

/**
 * Result that represents the status of sync of a single Tal
 *
 */
public class TalStatusResult extends ApiSingleResult<Tal> {

	public TalStatusResult(Tal tal) {
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

		return builder.build();
	}

}

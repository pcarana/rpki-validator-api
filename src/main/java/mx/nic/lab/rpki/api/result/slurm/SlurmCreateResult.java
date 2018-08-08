package mx.nic.lab.rpki.api.result.slurm;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiSingleResult;

/**
 * Result that represents the creation of a single SLURM Prefix or BGPsec
 *
 */
public class SlurmCreateResult extends ApiSingleResult<Long> {

	public SlurmCreateResult(Long createdId) {
		super();
		setApiObject(createdId);
	}

	@Override
	public JsonStructure toJsonStructure() {
		Long createdId = getApiObject();
		if (createdId == null) {
			return JsonObject.EMPTY_JSON_OBJECT;
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "id", createdId, true);
		return builder.build();
	}

}

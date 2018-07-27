package mx.nic.lab.rpki.api.result.slurm;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
import mx.nic.lab.rpki.db.pojo.SlurmPrefix;

/**
 * Result that represents a single SLURM Prefix
 *
 */
public class SlurmPrefixResult extends ApiSingleResult<SlurmPrefix> {

	public SlurmPrefixResult(SlurmPrefix slurmPrefix) {
		super();
		setApiObject(slurmPrefix);
	}

	@Override
	public JsonStructure toJsonStructure() {
		SlurmPrefix slurmPrefix = getApiObject();
		if (slurmPrefix == null) {
			return JsonObject.EMPTY_JSON_OBJECT;
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "id", slurmPrefix.getId(), true);
		addKeyValueToBuilder(builder, "asn", slurmPrefix.getAsn(), true);
		addKeyValueToBuilder(builder, "prefix", slurmPrefix.getPrefixText(), true);
		addKeyValueToBuilder(builder, "prefixLength", slurmPrefix.getPrefixLength(), true);
		addKeyValueToBuilder(builder, "prefixMaxLength", slurmPrefix.getPrefixMaxLength(), true);
		addKeyValueToBuilder(builder, "type", slurmPrefix.getType() == SlurmPrefix.TYPE_FILTER ? "filter" : "assertion",
				true);
		addKeyValueToBuilder(builder, "comment", slurmPrefix.getComment(), true);

		return builder.build();
	}

}

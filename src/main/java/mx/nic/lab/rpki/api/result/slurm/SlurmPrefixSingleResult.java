package mx.nic.lab.rpki.api.result.slurm;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
import mx.nic.lab.rpki.api.slurm.SlurmUtil;
import mx.nic.lab.rpki.db.pojo.SlurmPrefix;

/**
 * Result that represents a single SLURM Prefix
 *
 */
public class SlurmPrefixSingleResult extends ApiSingleResult<SlurmPrefix> {

	public SlurmPrefixSingleResult(SlurmPrefix slurmPrefix) {
		super();
		setApiObject(slurmPrefix);
	}

	@Override
	public JsonStructure toJsonStructure() {
		SlurmPrefix slurmPrefix = getApiObject();
		if (slurmPrefix == null) {
			return null;
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "id", slurmPrefix.getId(), true);
		addKeyValueToBuilder(builder, "type", slurmPrefix.getType(), true);
		builder.addAll(SlurmUtil.getPrefixBuilder(slurmPrefix));

		return builder.build();
	}
}

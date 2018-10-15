package mx.nic.lab.rpki.api.result.slurm;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
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
		addKeyValueToBuilder(builder, "asn", slurmPrefix.getAsn(), false);
		addKeyValueToBuilder(builder, "prefix", formatPrefix(slurmPrefix), false);
		addKeyValueToBuilder(builder, "maxPrefixLength", slurmPrefix.getPrefixMaxLength(), false);
		addKeyValueToBuilder(builder, "type", slurmPrefix.getType() == SlurmPrefix.TYPE_FILTER ? "filter" : "assertion",
				true);
		addKeyValueToBuilder(builder, "comment", slurmPrefix.getComment(), true);

		return builder.build();
	}

	private String formatPrefix(SlurmPrefix slurmPrefix) {
		if (slurmPrefix.getPrefixText() != null && !slurmPrefix.getPrefixText().isEmpty()) {
			return slurmPrefix.getPrefixText() + "/" + slurmPrefix.getPrefixLength();
		}
		return null;
	}
}

package mx.nic.lab.rpki.api.result.slurm;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
import mx.nic.lab.rpki.api.slurm.SlurmUtil;
import mx.nic.lab.rpki.db.pojo.SlurmBgpsec;

/**
 * Result that represents a single SLURM BGPsec
 *
 */
public class SlurmBgpsecSingleResult extends ApiSingleResult<SlurmBgpsec> {

	public SlurmBgpsecSingleResult(SlurmBgpsec slurmBgpsec) {
		super();
		setApiObject(slurmBgpsec);
	}

	@Override
	public JsonStructure toJsonStructure() {
		SlurmBgpsec slurmBgpsec = getApiObject();
		if (slurmBgpsec == null) {
			return null;
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "id", slurmBgpsec.getId(), true);
		addKeyValueToBuilder(builder, "type", slurmBgpsec.getType(), true);
		builder.addAll(SlurmUtil.getBgpsecBuilder(slurmBgpsec));

		return builder.build();
	}

}

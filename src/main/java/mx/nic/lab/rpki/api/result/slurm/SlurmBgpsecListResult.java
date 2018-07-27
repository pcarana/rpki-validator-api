package mx.nic.lab.rpki.api.result.slurm;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiListResult;
import mx.nic.lab.rpki.db.pojo.SlurmBgpsec;

/**
 * Result that represents a list of SLURM BGPsecs
 *
 */
public class SlurmBgpsecListResult extends ApiListResult<SlurmBgpsec> {

	public SlurmBgpsecListResult(List<SlurmBgpsec> slurmBgpsecs) {
		super();
		setApiObjects(slurmBgpsecs);
	}

	@Override
	public JsonStructure toJsonStructure() {
		if (getApiObjects() == null || getApiObjects().isEmpty()) {
			return JsonObject.EMPTY_JSON_ARRAY;
		}
		JsonArrayBuilder jsonBuilder = Json.createArrayBuilder();
		// Use the SlurmBgpsecResult implementation
		getApiObjects().forEach(obj -> {
			SlurmBgpsecResult temp = new SlurmBgpsecResult(obj);
			jsonBuilder.add(temp.toJsonStructure());
		});
		return jsonBuilder.build();
	}

}

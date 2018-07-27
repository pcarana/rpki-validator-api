package mx.nic.lab.rpki.api.result.slurm;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiListResult;
import mx.nic.lab.rpki.db.pojo.SlurmPrefix;

/**
 * Result that represents a list of SLURM Prefixes
 *
 */
public class SlurmPrefixListResult extends ApiListResult<SlurmPrefix> {

	public SlurmPrefixListResult(List<SlurmPrefix> slurmPrefixes) {
		super();
		setApiObjects(slurmPrefixes);
	}

	@Override
	public JsonStructure toJsonStructure() {
		if (getApiObjects() == null || getApiObjects().isEmpty()) {
			return JsonObject.EMPTY_JSON_ARRAY;
		}
		JsonArrayBuilder jsonBuilder = Json.createArrayBuilder();
		// Use the SlurmPrefixResult implementation
		getApiObjects().forEach(obj -> {
			SlurmPrefixResult temp = new SlurmPrefixResult(obj);
			jsonBuilder.add(temp.toJsonStructure());
		});
		return jsonBuilder.build();
	}

}

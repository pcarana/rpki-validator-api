package mx.nic.lab.rpki.api.result.slurm;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiListResult;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.SlurmPrefix;

/**
 * Result that represents a list of SLURM Prefixes
 *
 */
public class SlurmPrefixListResult extends ApiListResult<SlurmPrefix> {

	public SlurmPrefixListResult(ListResult<SlurmPrefix> listResult, PagingParameters pagingParameters) {
		super();
		setListResult(listResult);
		setPagingParameters(pagingParameters);
	}

	@Override
	public JsonStructure resultToJsonStructure() {
		if (getListResult() == null || getListResult().getResults().isEmpty()) {
			return JsonObject.EMPTY_JSON_ARRAY;
		}
		JsonArrayBuilder jsonBuilder = Json.createArrayBuilder();
		// Use the SlurmPrefixSingleResult implementation
		getListResult().getResults().forEach(obj -> {
			SlurmPrefixSingleResult temp = new SlurmPrefixSingleResult(obj);
			jsonBuilder.add(temp.toJsonStructure());
		});
		return jsonBuilder.build();
	}

}

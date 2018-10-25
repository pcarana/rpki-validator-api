package mx.nic.lab.rpki.api.result.slurm;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiListResult;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.SlurmBgpsec;

/**
 * Result that represents a list of SLURM BGPsecs
 *
 */
public class SlurmBgpsecListResult extends ApiListResult<SlurmBgpsec> {

	public SlurmBgpsecListResult(ListResult<SlurmBgpsec> listResult, PagingParameters pagingParameters) {
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
		// Use the SlurmBgpsecSingleResult implementation
		getListResult().getResults().forEach(obj -> {
			SlurmBgpsecSingleResult temp = new SlurmBgpsecSingleResult(obj);
			jsonBuilder.add(temp.toJsonStructure());
		});
		return jsonBuilder.build();
	}

}

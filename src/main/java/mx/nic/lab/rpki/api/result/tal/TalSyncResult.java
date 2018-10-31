package mx.nic.lab.rpki.api.result.tal;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiResult;

/**
 * Result that represents the execution result of a forced TAL sync
 *
 */
public class TalSyncResult extends ApiResult {

	private int exitStatus;

	public TalSyncResult(int exitStatus) {
		this.exitStatus = exitStatus;
	}

	@Override
	public JsonStructure toJsonStructure() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "execStatus", exitStatus, true);
		return builder.build();
	}
}

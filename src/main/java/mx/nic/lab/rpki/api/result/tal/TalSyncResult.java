package mx.nic.lab.rpki.api.result.tal;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.servlet.tal.TalSyncOneServlet.ExecutionStatus;

/**
 * Result that represents the execution result of a forced TAL sync
 *
 */
public class TalSyncResult extends ApiResult {

	private ExecutionStatus execStatus;

	public TalSyncResult(ExecutionStatus execStatus) {
		this.execStatus = execStatus;
	}

	@Override
	public JsonStructure toJsonStructure() {
		if (execStatus == null) {
			return null;
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "execStatus", execStatus.toString(), true);
		return builder.build();
	}
}

package mx.nic.lab.rpki.api.result.rtr;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiListResult;
import mx.nic.lab.rpki.db.pojo.RtrSession;

/**
 * Result that represents a list of RTR sessions
 *
 */
public class RtrSessionListResult extends ApiListResult<RtrSession> {

	public RtrSessionListResult(List<RtrSession> rtrSessions) {
		super();
		setApiObjects(rtrSessions);
	}

	@Override
	public JsonStructure toJsonStructure() {
		if (getApiObjects() == null || getApiObjects().isEmpty()) {
			return JsonObject.EMPTY_JSON_ARRAY;
		}
		JsonArrayBuilder jsonBuilder = Json.createArrayBuilder();
		getApiObjects().forEach(rtrSession -> {
			jsonBuilder.add(buildSingleRtrSession(rtrSession));
		});
		return jsonBuilder.build();
	}

	/**
	 * Builds a single {@link RtrSession} object as JSON
	 * 
	 * @param rtrSession
	 * @return
	 */
	private JsonStructure buildSingleRtrSession(RtrSession rtrSession) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "address", rtrSession.getAddress(), true);
		addKeyValueToBuilder(builder, "port", rtrSession.getPort(), true);
		addKeyValueToBuilder(builder, "status", rtrSession.getStatus(), true);
		addKeyValueToBuilder(builder, "lastRequest", rtrSession.getLastRequest(), true);
		addKeyValueToBuilder(builder, "lastResponse", rtrSession.getLastResponse(), true);
		return builder.build();
	}
}

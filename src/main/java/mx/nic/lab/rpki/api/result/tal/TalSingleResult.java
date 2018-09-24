package mx.nic.lab.rpki.api.result.tal;

import java.time.Instant;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
import mx.nic.lab.rpki.api.util.CMSUtil;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.pojo.TalUri;
import mx.nic.lab.rpki.db.pojo.ValidationCheck;
import mx.nic.lab.rpki.db.pojo.ValidationCheck.Status;
import mx.nic.lab.rpki.db.pojo.ValidationRun;

/**
 * Result that represents a single Tal
 *
 */
public class TalSingleResult extends ApiSingleResult<Tal> {

	public TalSingleResult(Tal tal) {
		super();
		setApiObject(tal);
	}

	@Override
	public JsonStructure toJsonStructure() {
		Tal tal = getApiObject();
		if (tal == null) {
			return null;
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "id", tal.getId(), true);
		addKeyValueToBuilder(builder, "publicKey", tal.getPublicKey(), true);
		addKeyValueToBuilder(builder, "loadedCer", CMSUtil.getCertAsJson(tal.getLoadedCer()), true);
		addKeyValueToBuilder(builder, "name", tal.getName(), true);
		buildTalUris(builder, tal);
		buildValidationRuns(builder, tal);

		return builder.build();
	}

	/**
	 * Adds the {@link TalUri} list of the {@link Tal} as a JSON Array to the
	 * <code>JsonObjectBuilder</code> sent
	 * 
	 * @param builder
	 * @param tal
	 */
	protected void buildTalUris(JsonObjectBuilder builder, Tal tal) {
		if (tal.getTalUris() == null || tal.getTalUris().isEmpty()) {
			builder.add("uris", JsonObject.EMPTY_JSON_ARRAY);
			return;
		}
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (TalUri talUri : tal.getTalUris()) {
			JsonObjectBuilder objBuilder = Json.createObjectBuilder();
			addKeyValueToBuilder(objBuilder, "location", talUri.getLocation(), true);
			arrayBuilder.add(objBuilder);
		}
		builder.add("uris", arrayBuilder);
	}

	/**
	 * Adds the {@link ValidationRun} list of the {@link Tal} as a JSON Array to the
	 * <code>JsonObjectBuilder</code> sent
	 * 
	 * @param builder
	 * @param tal
	 */
	protected void buildValidationRuns(JsonObjectBuilder builder, Tal tal) {
		if (tal.getValidationRuns() == null || tal.getValidationRuns().isEmpty()) {
			builder.add("validations", JsonObject.EMPTY_JSON_ARRAY);
			return;
		}
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (ValidationRun validationRun : tal.getValidationRuns()) {
			JsonObjectBuilder objBuilder = Json.createObjectBuilder();
			addKeyValueToBuilder(objBuilder, "status", validationRun.getStatus().toString(), true);
			addKeyValueToBuilder(objBuilder, "type", validationRun.getType().toString(), true);
			Instant completedAt = validationRun.getCompletedAt();
			addKeyValueToBuilder(objBuilder, "completedAt", completedAt != null ? completedAt.toString() : null, true);
			buildValidationChecks(objBuilder, validationRun);
			arrayBuilder.add(objBuilder);
		}
		builder.add("validations", arrayBuilder);
	}

	/**
	 * Adds the {@link ValidationCheck} list of the {@link ValidationRun} as a JSON
	 * Array to the <code>JsonObjectBuilder</code> sent
	 * 
	 * @param builder
	 * @param validationRun
	 */
	protected void buildValidationChecks(JsonObjectBuilder builder, ValidationRun validationRun) {
		JsonObjectBuilder checksBuilder = Json.createObjectBuilder();
		JsonArrayBuilder errorBuilder = Json.createArrayBuilder();
		JsonArrayBuilder warningBuilder = Json.createArrayBuilder();
		JsonArrayBuilder passedBuilder = Json.createArrayBuilder();
		if (validationRun.getValidationChecks() != null && !validationRun.getValidationChecks().isEmpty()) {
			for (ValidationCheck validationCheck : validationRun.getValidationChecks()) {
				JsonObjectBuilder checkBuilder = Json.createObjectBuilder();
				addKeyValueToBuilder(checkBuilder, "location", validationCheck.getLocation(), true);
				if (validationCheck.getStatus() != Status.PASSED) {
					// Prepare the key to search it at the bundles
					// The final value is #{key}.{status}{param0}{paramN}...
					StringBuilder keyBuilder = new StringBuilder();
					keyBuilder.append("#{");
					keyBuilder.append(validationCheck.getKey());
					keyBuilder.append(".");
					keyBuilder.append(validationCheck.getStatus().toString().toLowerCase());
					keyBuilder.append("}");
					if (validationCheck.getParameters() != null) {
						for (String parameter : validationCheck.getParameters()) {
							if (parameter != null && !parameter.trim().isEmpty()) {
								keyBuilder.append("{").append(parameter).append("}");
							}
						}
					}
					addKeyValueToBuilder(checkBuilder, "key", keyBuilder.toString(), true);
					switch (validationCheck.getStatus()) {
					case ERROR:
						errorBuilder.add(checkBuilder);
						break;
					case WARNING:
						warningBuilder.add(checkBuilder);
						break;
					default:
						break;
					}
				} else {
					passedBuilder.add(checkBuilder);
				}
			}
		}
		checksBuilder.add("error", errorBuilder);
		checksBuilder.add("warning", warningBuilder);
		checksBuilder.add("passed", passedBuilder);
		builder.add("checks", checksBuilder);
	}
}

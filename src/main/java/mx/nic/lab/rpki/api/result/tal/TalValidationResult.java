package mx.nic.lab.rpki.api.result.tal;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiListResult;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.ValidationCheck;
import mx.nic.lab.rpki.db.pojo.ValidationCheck.Status;

/**
 * Result that represents the Tal validations
 *
 */
public class TalValidationResult extends ApiListResult<ValidationCheck> {

	public TalValidationResult(ListResult<ValidationCheck> listResult, PagingParameters pagingParameters) {
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
		buildValidationChecks(jsonBuilder, getListResult().getResults());
		return jsonBuilder.build();
	}

	/**
	 * Adds the {@link ValidationCheck} list as a JSON Array to the
	 * <code>JsonArrayBuilder</code> sent
	 * 
	 * @param builder
	 * @param validationChecks
	 */
	protected void buildValidationChecks(JsonArrayBuilder builder, List<ValidationCheck> validationChecks) {
		for (ValidationCheck validationCheck : validationChecks) {
			JsonObjectBuilder checkBuilder = Json.createObjectBuilder();
			addKeyValueToBuilder(checkBuilder, "location", validationCheck.getLocation(), true);
			addKeyValueToBuilder(checkBuilder, "status", validationCheck.getStatus().toString().toLowerCase(), true);
			addKeyValueToBuilder(checkBuilder, "fileType", validationCheck.getFileType(), true);
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
				addKeyValueToBuilder(checkBuilder, "message", keyBuilder.toString(), true);
			}
			builder.add(checkBuilder);
		}
	}
}

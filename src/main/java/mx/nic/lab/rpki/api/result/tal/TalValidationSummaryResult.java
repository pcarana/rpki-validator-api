package mx.nic.lab.rpki.api.result.tal;

import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
import mx.nic.lab.rpki.db.pojo.ValidationCheck.Status;

/**
 * Result that represents the Tal validations
 *
 */
public class TalValidationSummaryResult extends ApiSingleResult<Map<Status, Map<String, Long>>> {

	public TalValidationSummaryResult(Map<Status, Map<String, Long>> summaryMap) {
		super();
		setApiObject(summaryMap);
	}

	@Override
	public JsonStructure toJsonStructure() {
		Map<Status, Map<String, Long>> summaryMap = getApiObject();
		if (summaryMap == null || summaryMap.isEmpty()) {
			return null;
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		for (Status status : summaryMap.keySet()) {
			Map<String, Long> fileTypeMap = summaryMap.get(status);
			JsonObjectBuilder objBuilder = Json.createObjectBuilder();
			for (String fileType : fileTypeMap.keySet()) {
				objBuilder.add(fileType, fileTypeMap.get(fileType));
			}
			builder.add(status.name().toLowerCase(), objBuilder);
		}
		return builder.build();
	}
}

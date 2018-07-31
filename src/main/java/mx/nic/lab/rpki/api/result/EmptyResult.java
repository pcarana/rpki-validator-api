package mx.nic.lab.rpki.api.result;

import javax.json.JsonObject;
import javax.json.JsonStructure;

/**
 * Generic response to return an empty JSON Object
 *
 */
public class EmptyResult extends ApiResult {

	@Override
	public JsonStructure toJsonStructure() {
		return JsonObject.EMPTY_JSON_OBJECT;
	}

}

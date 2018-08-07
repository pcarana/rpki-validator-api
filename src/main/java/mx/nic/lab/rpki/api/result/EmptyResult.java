package mx.nic.lab.rpki.api.result;

import javax.json.JsonStructure;

/**
 * Generic response to return an empty body
 *
 */
public class EmptyResult extends ApiResult {

	@Override
	public JsonStructure toJsonStructure() {
		return null;
	}

}

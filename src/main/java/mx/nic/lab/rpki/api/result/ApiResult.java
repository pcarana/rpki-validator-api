package mx.nic.lab.rpki.api.result;

import javax.json.JsonStructure;

/**
 * Generic result from the API
 *
 */
public abstract class ApiResult {

	/**
	 * Return the JSON of the result
	 * 
	 * @return {@link JsonStructure} of the object
	 */
	public abstract JsonStructure toJsonStructure();

}

package mx.nic.lab.rpki.api.result;

/**
 * Generic result from the API
 *
 */
public abstract class ApiResult {

	/**
	 * Return the JSON of the result
	 * 
	 * @return JSON as String
	 */
	public abstract String toJsonString();

}

package mx.nic.lab.rpki.db.pojo;

import javax.json.JsonObject;

/**
 * Abstract class to represent an API object
 *
 */
public abstract class ApiObject {

	/**
	 * Return the JSON representation of the object
	 * 
	 * @return JSON as JsonObject
	 */
	public abstract JsonObject toJsonObject();
}

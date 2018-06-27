package mx.nic.lab.rpki.db.pojo;

/**
 * Abstract class to represent an API object
 *
 */
public abstract class ApiObject {

	/**
	 * Return the JSON representation of the object
	 * 
	 * @return JSON as String
	 */
	public abstract String toJson();
}

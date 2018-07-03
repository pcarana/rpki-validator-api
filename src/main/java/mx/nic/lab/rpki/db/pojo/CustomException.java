package mx.nic.lab.rpki.db.pojo;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Exception representation as an API object, it has: code, title and
 * description
 *
 */
public class CustomException extends ApiObject {

	private int errorCode;
	private String errorTitle;
	private String errorDescription;

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorTitle() {
		return errorTitle;
	}

	public void setErrorTitle(String errorTitle) {
		this.errorTitle = errorTitle;
	}

	public String getErrorDescription() {
		return errorDescription;
	}

	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}

	@Override
	public JsonObject toJsonObject() {
		JsonObjectBuilder object = Json.createObjectBuilder();
		if (errorCode > 0) {
			object.add("errorCode", errorCode);
		}
		if (errorTitle != null) {
			object.add("title", errorTitle);
		}
		if (errorCode > 0 && errorDescription != null) {
			if (errorCode < 500)
				object.add("description", errorDescription);
		}
		return object.build();
	}

}

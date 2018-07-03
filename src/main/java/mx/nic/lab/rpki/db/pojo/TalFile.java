package mx.nic.lab.rpki.db.pojo;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Loaded file from a repository, represented as an API object
 *
 */
public class TalFile extends ApiObject {

	/**
	 * File ID
	 */
	private Long id;

	/**
	 * TAL ID from where the files belong
	 */
	private Long talId;

	/**
	 * File type (or extension)
	 */
	private String fileType;

	/**
	 * Status of the file (values: valid, invalid, warning)
	 */
	private String status;

	/**
	 * Detail when the status value is 'invalid' or 'warning'
	 */
	private String message;

	/**
	 * Location of the file at the repository (represented as a URI)
	 */
	private String location;

	@Override
	public String toString() {
		return toJsonObject().toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((talId == null) ? 0 : talId.hashCode());
		result = prime * result + ((fileType == null) ? 0 : fileType.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof TalFile))
			return false;
		TalFile other = (TalFile) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (talId == null) {
			if (other.talId != null)
				return false;
		} else if (!talId.equals(other.talId))
			return false;
		if (fileType == null) {
			if (other.fileType != null)
				return false;
		} else if (!fileType.equals(other.fileType))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		return true;
	}

	@Override
	public JsonObject toJsonObject() {
		JsonObjectBuilder object = Json.createObjectBuilder();
		object.add("id", id);
		object.add("talId", talId);
		object.add("fileType", fileType);
		object.add("status", status);
		object.add("message", message);
		object.add("location", location);
		return object.build();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTalId() {
		return talId;
	}

	public void setTalId(Long talId) {
		this.talId = talId;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
}

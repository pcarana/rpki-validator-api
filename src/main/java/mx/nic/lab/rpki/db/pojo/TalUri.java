package mx.nic.lab.rpki.db.pojo;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * URI configured at a TAL as an API object
 *
 */
public class TalUri extends ApiObject {

	/**
	 * TalUris ID
	 */
	private Long id;

	/**
	 * Related TAL ID
	 */
	private Long talId;

	/**
	 * URI value configured
	 */
	private String uriValue;

	/**
	 * Loaded CER from the URI configured, MAY be null if the URI wasn't loaded
	 */
	private String loadedCer;

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
		result = prime * result + ((uriValue == null) ? 0 : uriValue.hashCode());
		result = prime * result + ((loadedCer == null) ? 0 : loadedCer.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof TalUri))
			return false;
		TalUri other = (TalUri) obj;
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
		if (uriValue == null) {
			if (other.uriValue != null)
				return false;
		} else if (!uriValue.equals(other.uriValue))
			return false;
		if (loadedCer == null) {
			if (other.loadedCer != null)
				return false;
		} else if (!loadedCer.equals(other.loadedCer))
			return false;
		return true;
	}

	@Override
	public JsonObject toJsonObject() {
		JsonObjectBuilder object = Json.createObjectBuilder();
		object.add("id", id);
		object.add("talId", talId);
		object.add("uriValue", uriValue);
		object.add("loadedCer", loadedCer);
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

	public String getUriValue() {
		return uriValue;
	}

	public void setUriValue(String uriValue) {
		this.uriValue = uriValue;
	}

	public String getLoadedCer() {
		return loadedCer;
	}

	public void setLoadedCer(String loadedCer) {
		this.loadedCer = loadedCer;
	}
}

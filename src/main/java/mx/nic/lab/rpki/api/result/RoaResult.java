package mx.nic.lab.rpki.api.result;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.db.pojo.Roa;

/**
 * Result that represents a single Roa
 *
 */
public class RoaResult extends ApiSingleResult<Roa> {

	public RoaResult(Roa roa) {
		super();
		setApiObject(roa);
	}

	@Override
	public JsonStructure toJsonStructure() {
		Roa roa = getApiObject();
		JsonObjectBuilder object = Json.createObjectBuilder();
		if (roa.getId() != null) {
			object.add("id", roa.getId());
		} else {
			object.addNull("id");
		}
		return object.build();
	}
}

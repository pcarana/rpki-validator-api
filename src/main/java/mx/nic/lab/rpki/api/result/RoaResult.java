package mx.nic.lab.rpki.api.result;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.db.pojo.Gbr;
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
		if (roa == null) {
			return JsonObject.EMPTY_JSON_OBJECT;
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if (roa.getId() != null) {
			builder.add("id", roa.getId());
		} else {
			builder.addNull("id");
		}
		if (roa.getAsn() != null) {
			builder.add("asn", roa.getAsn());
		} else {
			builder.addNull("asn");
		}
		if (roa.getPrefixText() != null) {
			builder.add("prefix", roa.getPrefixText());
		} else {
			builder.addNull("prefix");
		}
		if (roa.getPrefixLength() != null) {
			builder.add("prefixLength", roa.getPrefixLength());
		} else {
			builder.addNull("prefixLength");
		}
		if (roa.getPrefixMaxLength() != null) {
			builder.add("prefixMaxLength", roa.getPrefixMaxLength());
		} else {
			builder.addNull("prefixMaxLength");
		}
		// FIXME Return as valid JSON maybe?
		if (roa.getCmsData() != null) {
			builder.add("cms", roa.getCmsData().toString());
		} else {
			builder.addNull("cms");
		}
		buildRoaGbrs(builder, roa);

		return builder.build();
	}

	/**
	 * Adds the {@link Gbr} list of the {@link Roa} as a JSON Array to the
	 * <code>JsonObjectBuilder</code> sent
	 * 
	 * @param builder
	 * @param roa
	 */
	protected void buildRoaGbrs(JsonObjectBuilder builder, Roa roa) {
		if (roa.getGbrs() == null || roa.getGbrs().isEmpty()) {
			builder.add("gbr", JsonObject.EMPTY_JSON_ARRAY);
			return;
		}
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (Gbr gbr : roa.getGbrs()) {
			JsonObjectBuilder objBuilder = Json.createObjectBuilder();
			if (gbr.getVcard() != null) {
				builder.add("vcard", gbr.getVcard());
			} else {
				builder.addNull("vcard");
			}
			// FIXME Return as valid JSON maybe?
			if (gbr.getCmsData() != null) {
				builder.add("cms", roa.getCmsData().toString());
			} else {
				builder.addNull("cms");
			}
			arrayBuilder.add(objBuilder);
		}
		builder.add("gbr", arrayBuilder);
	}
}

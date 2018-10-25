package mx.nic.lab.rpki.api.result.roa;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiListResult;
import mx.nic.lab.rpki.db.pojo.Gbr;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.Roa;

/**
 * Result that represents a list of Roas
 *
 */
public class RoaListResult extends ApiListResult<Roa> {

	public RoaListResult(ListResult<Roa> listResult, PagingParameters pagingParameters) {
		super();
		setListResult(listResult);
		setPagingParameters(pagingParameters);
	}

	@Override
	public JsonStructure resultToJsonStructure() {
		if (getListResult() == null || getListResult().getResults().isEmpty()) {
			return JsonObject.EMPTY_JSON_ARRAY;
		}
		JsonArrayBuilder jsonBuilder = Json.createArrayBuilder();
		getListResult().getResults().forEach(roa -> {
			jsonBuilder.add(buildSingleRoa(roa));
		});
		return jsonBuilder.build();
	}

	private JsonStructure buildSingleRoa(Roa roa) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "id", roa.getId(), true);
		addKeyValueToBuilder(builder, "asn", roa.getAsn(), true);
		addKeyValueToBuilder(builder, "prefix", roa.getPrefixText(), true);
		addKeyValueToBuilder(builder, "prefixLength", roa.getPrefixLength(), true);
		addKeyValueToBuilder(builder, "prefixMaxLength", roa.getPrefixMaxLength(), true);
		addKeyValueToBuilder(builder, "prefixFamily", roa.getPrefixFamily(), true);
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
			builder.add("gbrs", JsonObject.EMPTY_JSON_ARRAY);
			return;
		}
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (Gbr gbr : roa.getGbrs()) {
			JsonObjectBuilder objBuilder = Json.createObjectBuilder();
			// The id is omitted since is used for internal purposes
			addKeyValueToBuilder(objBuilder, "vcard", gbr.getVcard(), true);
			arrayBuilder.add(objBuilder);
		}
		builder.add("gbrs", arrayBuilder);
	}
}

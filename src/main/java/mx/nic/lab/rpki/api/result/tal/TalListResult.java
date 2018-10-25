package mx.nic.lab.rpki.api.result.tal;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiListResult;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.pojo.TalUri;

/**
 * Result that represents a list of Tals
 *
 */
public class TalListResult extends ApiListResult<Tal> {

	public TalListResult(ListResult<Tal> listResult, PagingParameters pagingParameters) {
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
		getListResult().getResults().forEach(tal -> {
			jsonBuilder.add(buildSingleTal(tal));
		});
		return jsonBuilder.build();
	}

	/**
	 * Builds a single {@link Tal} object as JSON
	 * 
	 * @param tal
	 * @return
	 */
	private JsonStructure buildSingleTal(Tal tal) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "id", tal.getId(), true);
		addKeyValueToBuilder(builder, "publicKey", tal.getPublicKey(), true);
		addKeyValueToBuilder(builder, "name", tal.getName(), true);
		buildTalUris(builder, tal);

		return builder.build();
	}

	/**
	 * Adds the {@link TalUri} list of the {@link Tal} as a JSON Array to the
	 * <code>JsonObjectBuilder</code> sent
	 * 
	 * @param builder
	 * @param tal
	 */
	protected void buildTalUris(JsonObjectBuilder builder, Tal tal) {
		if (tal.getTalUris() == null || tal.getTalUris().isEmpty()) {
			builder.add("uris", JsonObject.EMPTY_JSON_ARRAY);
			return;
		}
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (TalUri talUri : tal.getTalUris()) {
			JsonObjectBuilder objBuilder = Json.createObjectBuilder();
			addKeyValueToBuilder(objBuilder, "location", talUri.getLocation(), true);
			arrayBuilder.add(objBuilder);
		}
		builder.add("uris", arrayBuilder);
	}
}

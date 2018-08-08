package mx.nic.lab.rpki.api.result.tal;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiListResult;
import mx.nic.lab.rpki.api.util.CMSUtil;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.pojo.TalUri;

/**
 * Result that represents a list of Tals
 *
 */
public class TalListResult extends ApiListResult<Tal> {

	public TalListResult(List<Tal> tals) {
		super();
		setApiObjects(tals);
	}

	@Override
	public JsonStructure toJsonStructure() {
		if (getApiObjects() == null) {
			return JsonObject.EMPTY_JSON_ARRAY;
		}
		JsonArrayBuilder jsonBuilder = Json.createArrayBuilder();
		getApiObjects().forEach(tal -> {
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
		addKeyValueToBuilder(builder, "lastSync", tal.getLastSync(), true);
		addKeyValueToBuilder(builder, "publicKey", tal.getPublicKey(), true);
		addKeyValueToBuilder(builder, "status", tal.getStatus(), true);
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
			addKeyValueToBuilder(objBuilder, "value", talUri.getValue(), true);
			addKeyValueToBuilder(objBuilder, "loadedCer", CMSUtil.getCertAsJson(talUri.getLoadedCer()), true);
			addKeyValueToBuilder(objBuilder, "loaded", talUri.getLoaded(), true);
			arrayBuilder.add(objBuilder);
		}
		builder.add("uris", arrayBuilder);
	}
}

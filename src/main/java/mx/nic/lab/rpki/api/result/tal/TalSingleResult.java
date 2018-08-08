package mx.nic.lab.rpki.api.result.tal;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
import mx.nic.lab.rpki.api.util.CMSUtil;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.pojo.TalFile;
import mx.nic.lab.rpki.db.pojo.TalUri;

/**
 * Result that represents a single Tal
 *
 */
public class TalSingleResult extends ApiSingleResult<Tal> {

	public TalSingleResult(Tal tal) {
		super();
		setApiObject(tal);
	}

	@Override
	public JsonStructure toJsonStructure() {
		Tal tal = getApiObject();
		if (tal == null) {
			return JsonObject.EMPTY_JSON_OBJECT;
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "id", tal.getId(), true);
		addKeyValueToBuilder(builder, "lastSync", tal.getLastSync(), true);
		addKeyValueToBuilder(builder, "publicKey", tal.getPublicKey(), true);
		addKeyValueToBuilder(builder, "status", tal.getStatus(), true);
		addKeyValueToBuilder(builder, "name", tal.getName(), true);
		buildTalUris(builder, tal);
		buildTalFiles(builder, tal);

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
			builder.add("uri", JsonObject.EMPTY_JSON_ARRAY);
			return;
		}
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (TalUri talUri : tal.getTalUris()) {
			JsonObjectBuilder objBuilder = Json.createObjectBuilder();
			addKeyValueToBuilder(objBuilder, "id", talUri.getId(), true);
			addKeyValueToBuilder(objBuilder, "value", talUri.getValue(), true);
			addKeyValueToBuilder(objBuilder, "loadedCer", CMSUtil.getCertAsJson(talUri.getLoadedCer()), true);
			addKeyValueToBuilder(objBuilder, "loaded", talUri.getLoaded(), true);
			arrayBuilder.add(objBuilder);
		}
		builder.add("uri", arrayBuilder);
	}

	/**
	 * Adds the {@link TalFile} list of the {@link Tal} as a JSON Array to the
	 * <code>JsonObjectBuilder</code> sent
	 * 
	 * @param builder
	 * @param tal
	 */
	protected void buildTalFiles(JsonObjectBuilder builder, Tal tal) {
		if (tal.getTalFiles() == null || tal.getTalFiles().isEmpty()) {
			builder.add("files", JsonObject.EMPTY_JSON_ARRAY);
			return;
		}
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (TalFile talFile : tal.getTalFiles()) {
			JsonObjectBuilder objBuilder = Json.createObjectBuilder();
			addKeyValueToBuilder(objBuilder, "id", talFile.getId(), true);
			addKeyValueToBuilder(objBuilder, "fileType", talFile.getFileType(), true);
			addKeyValueToBuilder(objBuilder, "status", talFile.getStatus().toString(), true);
			addKeyValueToBuilder(objBuilder, "message", talFile.getMessage(), true);
			addKeyValueToBuilder(objBuilder, "location", talFile.getLocation(), true);
			arrayBuilder.add(objBuilder);
		}
		builder.add("files", arrayBuilder);
	}
}

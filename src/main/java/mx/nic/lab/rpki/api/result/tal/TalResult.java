package mx.nic.lab.rpki.api.result.tal;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.pojo.TalFile;
import mx.nic.lab.rpki.db.pojo.TalUri;

/**
 * Result that represents a single Tal
 *
 */
public class TalResult extends ApiSingleResult<Tal> {

	public TalResult(Tal tal) {
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
		if (tal.getId() != null) {
			builder.add("id", tal.getId());
		} else {
			builder.addNull("id");
		}
		if (tal.getLastSync() != null) {
			builder.add("lastSync", tal.getLastSync());
		} else {
			builder.addNull("lastSync");
		}
		if (tal.getPublicKey() != null) {
			builder.add("publicKey", tal.getPublicKey());
		} else {
			builder.addNull("publicKey");
		}
		if (tal.getStatus() != null) {
			builder.add("status", tal.getStatus());
		} else {
			builder.addNull("status");
		}
		if (tal.getName() != null) {
			builder.add("name", tal.getName());
		} else {
			builder.addNull("name");
		}
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
			if (talUri.getId() != null) {
				builder.add("id", talUri.getId());
			} else {
				builder.addNull("id");
			}
			if (talUri.getValue() != null) {
				builder.add("value", talUri.getValue());
			} else {
				builder.addNull("value");
			}
			// FIXME Return as valid JSON maybe?
			if (talUri.getLoadedCer() != null) {
				builder.add("loadedCer", talUri.getLoadedCer().toString());
			} else {
				builder.addNull("loadedCer");
			}
			if (talUri.getLoaded() != null) {
				builder.add("loaded", talUri.getLoaded());
			} else {
				builder.addNull("loaded");
			}
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
			if (talFile.getId() != null) {
				builder.add("id", talFile.getId());
			} else {
				builder.addNull("id");
			}
			if (talFile.getFileType() != null) {
				builder.add("fileType", talFile.getFileType());
			} else {
				builder.addNull("fileType");
			}
			if (talFile.getStatus() != null) {
				builder.add("status", talFile.getStatus().toString());
			} else {
				builder.addNull("status");
			}
			if (talFile.getMessage() != null) {
				builder.add("message", talFile.getMessage());
			} else {
				builder.addNull("message");
			}
			if (talFile.getLocation() != null) {
				builder.add("location", talFile.getLocation());
			} else {
				builder.addNull("location");
			}
			arrayBuilder.add(objBuilder);
		}
		builder.add("files", arrayBuilder);
	}
}

package mx.nic.lab.rpki.api.result.slurm;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
import mx.nic.lab.rpki.db.pojo.Slurm;
import mx.nic.lab.rpki.db.pojo.SlurmBgpsec;
import mx.nic.lab.rpki.db.pojo.SlurmPrefix;

/**
 * Result that represents the complete SLURM
 *
 */
public class SlurmResult extends ApiSingleResult<Slurm> {

	public SlurmResult(Slurm slurm) {
		super();
		setApiObject(slurm);
	}

	@Override
	public JsonStructure toJsonStructure() {
		Slurm slurm = getApiObject();
		if (slurm == null) {
			return null;
		}
		// Response conforming the original SLURM as indicated at RFC 8416
		JsonObjectBuilder slurmBuilder = Json.createObjectBuilder();
		JsonObjectBuilder validationOutputFilters = Json.createObjectBuilder();
		JsonObjectBuilder locallyAddedAssertions = Json.createObjectBuilder();
		JsonArrayBuilder prefixFilters = Json.createArrayBuilder();
		JsonArrayBuilder prefixAssertions = Json.createArrayBuilder();
		JsonArrayBuilder bgpsecFilters = Json.createArrayBuilder();
		JsonArrayBuilder bgpsecAssertions = Json.createArrayBuilder();

		slurm.getValidationOutputFilters().getPrefixes().forEach((prefix) -> {
			prefixFilters.add(getFormattedPrefix(prefix));
		});
		slurm.getValidationOutputFilters().getBgpsecs().forEach((bgpsec) -> {
			bgpsecFilters.add(getFormattedBgpsec(bgpsec));
		});
		slurm.getLocallyAddedAssertions().getPrefixes().forEach((prefix) -> {
			prefixAssertions.add(getFormattedPrefix(prefix));
		});
		slurm.getLocallyAddedAssertions().getBgpsecs().forEach((bgpsec) -> {
			bgpsecAssertions.add(getFormattedBgpsec(bgpsec));
		});

		validationOutputFilters.add("prefixFilters", prefixFilters);
		validationOutputFilters.add("bgpsecFilters", bgpsecFilters);

		locallyAddedAssertions.add("prefixAssertions", prefixAssertions);
		locallyAddedAssertions.add("bgpsecAssertions", bgpsecAssertions);

		slurmBuilder.add("slurmVersion", slurm.getSlurmVersion());
		slurmBuilder.add("validationOutputFilters", validationOutputFilters);
		slurmBuilder.add("locallyAddedAssertions", locallyAddedAssertions);

		return slurmBuilder.build();
	}

	private JsonObject getFormattedPrefix(SlurmPrefix prefix) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if (prefix.getPrefixText() != null && !prefix.getPrefixText().isEmpty()) {
			String value = prefix.getPrefixText().concat("/").concat(prefix.getPrefixLength().toString());
			addKeyValueToBuilder(builder, "prefix", value, false);
		}
		addKeyValueToBuilder(builder, "asn", prefix.getAsn(), false);
		addKeyValueToBuilder(builder, "maxPrefixLength", prefix.getPrefixMaxLength(), false);
		addKeyValueToBuilder(builder, "comment", prefix.getComment(), false);
		return builder.build();
	}

	private JsonObject getFormattedBgpsec(SlurmBgpsec bgpsec) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "asn", bgpsec.getAsn(), false);
		addKeyValueToBuilder(builder, "SKI", bgpsec.getSki(), false);
		addKeyValueToBuilder(builder, "routerPublicKey", bgpsec.getRouterPublicKey(), false);
		addKeyValueToBuilder(builder, "comment", bgpsec.getComment(), false);
		return builder.build();
	}
}

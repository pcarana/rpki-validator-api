package mx.nic.lab.rpki.api.result.validate;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
import mx.nic.lab.rpki.api.result.roa.RoaSingleResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmPrefixSingleResult;
import mx.nic.lab.rpki.db.pojo.RouteValidation;

/**
 * Result that represents a route validation
 *
 */
public class RouteValidationResult extends ApiSingleResult<RouteValidation> {

	public RouteValidationResult(RouteValidation routeValidation) {
		super();
		setApiObject(routeValidation);
	}

	@Override
	public JsonStructure toJsonStructure() {
		RouteValidation routeValidation = getApiObject();
		if (routeValidation == null) {
			return null;
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "validityState", routeValidation.getValidityState(), true);
		addKeyValueToBuilder(builder, "prefixState", routeValidation.getPrefixState(), true);
		addKeyValueToBuilder(builder, "asState", routeValidation.getAsState(), true);
		addKeyValueToBuilder(builder, "fullCheck", routeValidation.getFullCheck(), true);
		if (routeValidation.getRoaMatch() != null) {
			// Use the existent ROA result
			RoaSingleResult roaResult = new RoaSingleResult(routeValidation.getRoaMatch());
			addKeyValueToBuilder(builder, "match", roaResult.toJsonStructure(), false);
		}
		if (routeValidation.getSlurmMatch() != null) {
			SlurmPrefixSingleResult slurmResult = new SlurmPrefixSingleResult(routeValidation.getSlurmMatch());
			addKeyValueToBuilder(builder, "slurmMatch", slurmResult.toJsonStructure(), false);
		}
		return builder.build();
	}
}

package mx.nic.lab.rpki.api.result.roa;

import mx.nic.lab.rpki.api.result.ApiListResultRPSL;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.Roa;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;



/**
 *
 * Result that represents a list of Roas in RPSL format
 *
 */
public class RoaListResultRPSL extends ApiListResultRPSL<Roa> {


	public RoaListResultRPSL(ListResult<Roa> listResult, PagingParameters pagingParameters) {
		super();
		setListResult(listResult);
		setPagingParameters(pagingParameters);
	}

	/**
	 * Converts a <code>Roa</code> to a String in RPSL format
	 * @param roa Roa to be converted
	 * @return String representing the Roa in RPSL format
	 */
	@Override
	protected String getRPSLString(Roa roa) {
		String now = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(DateTime.now());

		return "\n" + "route" + (6 == roa.getPrefixFamily() ? "6" : StringUtils.EMPTY) + ": " + roa.getPrefixText() + "\n" +
				"origin: AS" + roa.getAsn() + "\n" +
				"descr: " + "exported from FORT RPKI validator" + "\n" +
				"mnt-by: NA" + "\n" +
				"created: " + now + "\n" +
				"last-modified: " + now + "\n" +
				"source: " + "FORT RPKI validator" +  "\n"
				;
	}

}

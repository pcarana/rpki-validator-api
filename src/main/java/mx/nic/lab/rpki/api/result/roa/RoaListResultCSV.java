package mx.nic.lab.rpki.api.result.roa;

import mx.nic.lab.rpki.api.result.ApiListResultCSV;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.pojo.Roa;

/**
 *
 * Result that represents a list of Roas in CSV format
 *
 */
public class RoaListResultCSV extends ApiListResultCSV<Roa> {


	public RoaListResultCSV(ListResult<Roa> listResult, PagingParameters pagingParameters) {
		super();
		setListResult(listResult);
		setPagingParameters(pagingParameters);
	}


	@Override
	protected String[] getLine(Roa roa) {
		return new String[]{
				roa.getId().toString(),
				roa.getAsn().toString(),
				roa.getPrefixText(),
				roa.getPrefixLength().toString(),
				roa.getPrefixMaxLength().toString(),
				roa.getPrefixFamily().toString()
		};
	}

	@Override
	protected String[] getTilte() {
		return new String[]{"id", "asn", "prefix", "prefixLength", "prefixMaxLength", "prefixFamily"};
	}
}

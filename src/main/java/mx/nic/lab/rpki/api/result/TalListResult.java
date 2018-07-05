package mx.nic.lab.rpki.api.result;

import java.util.List;

import mx.nic.lab.rpki.db.pojo.Tal;

/**
 * Result that represents a list of Tals
 *
 */
public class TalListResult extends ApiListResult {

	public TalListResult(List<Tal> tals) {
		super();
		setApiObjects(tals);
	}

}

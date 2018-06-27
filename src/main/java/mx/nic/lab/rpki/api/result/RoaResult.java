package mx.nic.lab.rpki.api.result;

import mx.nic.lab.rpki.db.pojo.Roa;

/**
 * Result that represents a single Roa
 *
 */
public class RoaResult extends ApiSingleResult {

	public RoaResult(Roa roa) {
		super();
		setApiObject(roa);
	}

}

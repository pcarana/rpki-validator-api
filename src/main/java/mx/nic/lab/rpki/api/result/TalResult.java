package mx.nic.lab.rpki.api.result;

import mx.nic.lab.rpki.db.pojo.Tal;

/**
 * Result that represents a single Tal
 *
 */
public class TalResult extends ApiSingleResult {

	public TalResult(Tal tal) {
		super();
		setApiObject(tal);
	}

}

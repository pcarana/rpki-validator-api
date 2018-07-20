package mx.nic.lab.rpki.api.servlet.tal;

import mx.nic.lab.rpki.api.servlet.DataAccessServlet;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.TalDAO;

/**
 * Abstract class to load TAL DAO, must be used by all the servlets that respond
 * TAL objects
 *
 */
public abstract class TalServlet extends DataAccessServlet<TalDAO> {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected TalDAO initAccessDAO() throws ApiDataAccessException {
		return DataAccessService.getTalDAO();
	}

}

package mx.nic.lab.rpki.api.servlet.roa;

import mx.nic.lab.rpki.api.servlet.DataAccessServlet;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.RoaDAO;

/**
 * Abstract class to load ROA DAO, must be used by all the servlets that respond
 * ROA objects
 *
 */
public abstract class RoaServlet extends DataAccessServlet<RoaDAO> {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected RoaDAO initAccessDAO() throws ApiDataAccessException {
		return DataAccessService.getRoaDAO();
	}

}

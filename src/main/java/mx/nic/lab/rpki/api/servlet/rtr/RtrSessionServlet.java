package mx.nic.lab.rpki.api.servlet.rtr;

import mx.nic.lab.rpki.api.servlet.DataAccessServlet;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.RtrSessionDAO;

/**
 * Abstract class to load RTR session DAO, must be used by all the servlets that
 * respond RTR session objects
 *
 */
public abstract class RtrSessionServlet extends DataAccessServlet<RtrSessionDAO> {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected RtrSessionDAO initAccessDAO() throws ApiDataAccessException {
		return DataAccessService.getRtrSessionDAO();
	}

}

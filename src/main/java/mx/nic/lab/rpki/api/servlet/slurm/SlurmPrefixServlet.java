package mx.nic.lab.rpki.api.servlet.slurm;

import mx.nic.lab.rpki.api.servlet.DataAccessServlet;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.SlurmPrefixDAO;

/**
 * Abstract class to load SLURM Prefix DAO, must be used by all the servlets
 * that respond SLURM Prefix objects
 *
 */
public abstract class SlurmPrefixServlet extends DataAccessServlet<SlurmPrefixDAO> {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constant to define the path to get/post/delete prefix filters, it's expected
	 * to come as part of the requested URI. E.g. to get all the filters: GET
	 * {slurm_prefix_path}/filter
	 */
	protected static final String FILTER_SERVICE = "filter";

	/**
	 * Constant to define the path to get/post/delete prefix assertions, it's
	 * expected to come as part of the requested URI. E.g. to get all the
	 * assertions: GET {slurm_prefix_path}/assertion
	 */
	protected static final String ASSERTION_SERVICE = "assertion";

	@Override
	protected SlurmPrefixDAO initAccessDAO() throws ApiDataAccessException {
		return DataAccessService.getSlurmPrefixDAO();
	}

}

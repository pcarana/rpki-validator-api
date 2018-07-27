package mx.nic.lab.rpki.api.servlet.slurm;

import mx.nic.lab.rpki.api.servlet.DataAccessServlet;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.SlurmBgpsecDAO;

/**
 * Abstract class to load SLURM BGPsec DAO, must be used by all the servlets
 * that respond SLURM BGPsec objects
 *
 */
public abstract class SlurmBgpsecServlet extends DataAccessServlet<SlurmBgpsecDAO> {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constant to define the path to get/post/delete bgpsec filters, it's expected
	 * to come as part of the requested URI. E.g. to get all the filters: GET
	 * {slurm_bgpsec_path}/filter
	 */
	protected static final String FILTER_SERVICE = "filter";

	/**
	 * Constant to define the path to get/post/delete bgpsec assertions, it's
	 * expected to come as part of the requested URI. E.g. to get all the
	 * assertions: GET {slurm_bgpsec_path}/assertion
	 */
	protected static final String ASSERTION_SERVICE = "assertion";

	@Override
	protected SlurmBgpsecDAO initAccessDAO() throws ApiDataAccessException {
		return DataAccessService.getSlurmBgpsecDAO();
	}

}

package mx.nic.lab.rpki.api.servlet.slurm;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmPrefixListResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.SlurmPrefix;
import mx.nic.lab.rpki.db.spi.SlurmPrefixDAO;

/**
 * Servlet to provide all the SLURM Prefixes
 *
 */
@WebServlet(name = "slurmPrefixList", value = { "/slurm/prefix" })
public class SlurmPrefixListServlet extends SlurmPrefixServlet {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, SlurmPrefixDAO dao)
			throws HttpException, ApiDataAccessException {
		List<SlurmPrefix> slurmPrefixes = dao.getAll();
		return new SlurmPrefixListResult(slurmPrefixes);
	}

	@Override
	protected String getServedObjectName() {
		return "slurmPrefixList";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.GET);
	}

}

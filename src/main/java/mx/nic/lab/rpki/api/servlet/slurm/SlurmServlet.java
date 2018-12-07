package mx.nic.lab.rpki.api.servlet.slurm;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmResult;
import mx.nic.lab.rpki.api.servlet.DataAccessServlet;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.Slurm;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.SlurmDAO;

/**
 * Servlet to provide the complete SLURM
 *
 */
@WebServlet(name = "slurm", value = { "/slurm" })
public class SlurmServlet extends DataAccessServlet<SlurmDAO> {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, SlurmDAO dao)
			throws HttpException, ApiDataAccessException {
		Slurm slurm = dao.getAll();
		return new SlurmResult(slurm);
	}

	@Override
	protected String getServedObjectName() {
		return "slurm";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.GET);
	}

	@Override
	protected SlurmDAO initAccessDAO() throws ApiDataAccessException {
		return DataAccessService.getSlurmDAO();
	}

}

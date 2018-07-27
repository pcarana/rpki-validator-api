package mx.nic.lab.rpki.api.servlet.slurm;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.slurm.SlurmBgpsecListResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.http.HttpException;
import mx.nic.lab.rpki.db.pojo.SlurmBgpsec;
import mx.nic.lab.rpki.db.spi.SlurmBgpsecDAO;

/**
 * Servlet to provide all the SLURM Bgpsecs
 *
 */
@WebServlet(name = "slurmBgpsecList", value = { "/slurm/bgpsec" })
public class SlurmBgpsecListServlet extends SlurmBgpsecServlet {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, SlurmBgpsecDAO dao)
			throws HttpException, ApiDataAccessException {
		List<SlurmBgpsec> slurmBgpsecs = dao.getAll();
		return new SlurmBgpsecListResult(slurmBgpsecs);
	}

	@Override
	protected String getServedObjectName() {
		return "slurmBgpsecList";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.GET);
	}

}

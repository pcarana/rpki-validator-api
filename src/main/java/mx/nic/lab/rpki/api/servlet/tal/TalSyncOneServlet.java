package mx.nic.lab.rpki.api.servlet.tal;

import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.config.ApiConfiguration;
import mx.nic.lab.rpki.api.exception.BadRequestException;
import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.tal.TalSyncResult;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.spi.TalDAO;
import net.ripe.rpki.commons.rsync.Command;

/**
 * Servlet to synchronize a TAL by its ID
 *
 */
@WebServlet(name = "talSync", value = { "/tal/sync/*" })
public class TalSyncOneServlet extends TalServlet {

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, TalDAO dao)
			throws HttpException, ApiDataAccessException {
		List<String> additionalPathInfo = Util.getAdditionaPathInfo(request, 1, false);
		Long id = null;
		try {
			id = Long.parseLong(additionalPathInfo.get(0));
		} catch (NumberFormatException e) {
			throw new BadRequestException("#{error.invalidId}", e);
		}
		Tal tal = dao.getById(id);
		if (tal == null) {
			return null;
		}
		int exitStatus = runValidatorCommand(tal.getName());
		return new TalSyncResult(exitStatus);
	}

	@Override
	protected String getServedObjectName() {
		return "talSyncOne";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.POST);
	}

	/**
	 * Run the OS command to force a TAL synchronization
	 * 
	 * @param talName
	 * @return exit status of the execution
	 */
	private int runValidatorCommand(String talName) {
		String talLocation = Paths.get(ApiConfiguration.getTalsLocation(), talName.concat(".tal")).toString();
		List<String> commandWArgs = new ArrayList<>();
		commandWArgs.add(ApiConfiguration.getValidatorCommand());
		commandWArgs.add(MessageFormat.format(ApiConfiguration.getValidatorArgSyncTal(), talLocation));
		String argOpts = ApiConfiguration.getValidatorArgSyncTalOpts();
		if (argOpts != null) {
			commandWArgs.add(argOpts);
		}
		Command cmd = Util.createAndExecCommand(commandWArgs);
		return cmd.getExitStatus();
	}
}

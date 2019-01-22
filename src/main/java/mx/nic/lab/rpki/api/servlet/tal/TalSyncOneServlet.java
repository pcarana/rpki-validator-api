package mx.nic.lab.rpki.api.servlet.tal;

import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	 * Sync execution status
	 *
	 */
	public enum ExecutionStatus {
		REQUESTED("requested"), // The sync was successfully requested
		RUNNING("running"), // The sync is running
		REQUEST_ERROR("request-error"), // There was an error requesting the sync
		FINISHED_OK("finished-ok"), // The sync has finished successfully
		FINISHED_ERROR("finished-error"), // The sync has finished with an error
		NOT_RUNNING("not-running"); // The sync isn't running nor had been requested

		private final String description;

		private ExecutionStatus(String description) {
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}
	}

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Map to store the relation between a TAL and its sync requests
	 */
	private static final Map<Long, Command> executionMap = new HashMap<Long, Command>();

	/**
	 * Expected result of a successful execution
	 */
	private static final int EXEC_SUCCESS = 0;

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
		if (RequestMethod.GET.equals(requestMethod)) {
			return new TalSyncResult(handleGet(tal));
		}
		if (RequestMethod.POST.equals(requestMethod)) {
			return new TalSyncResult(handlePost(tal));
		}
		return null;
	}

	@Override
	protected String getServedObjectName() {
		return "talSyncOne";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.GET, RequestMethod.POST);
	}

	/**
	 * Check the execution status of a sync request
	 * 
	 * @param tal
	 * @return {@link ExecutionStatus} of the sync request
	 */
	private ExecutionStatus handleGet(Tal tal) {
		Long talId = tal.getId();
		if (!executionMap.containsKey(talId)) {
			return ExecutionStatus.NOT_RUNNING;
		}
		Command cmd = executionMap.get(talId);
		if (cmd.isAlive()) {
			return ExecutionStatus.RUNNING;
		}
		if (cmd.getExitStatus() == EXEC_SUCCESS) {
			return ExecutionStatus.FINISHED_OK;
		}
		return ExecutionStatus.FINISHED_ERROR;
	}

	/**
	 * Run the OS command to force a TAL synchronization
	 * 
	 * @param tal
	 * @return {@link ExecutionStatus} of the sync request
	 */
	private ExecutionStatus handlePost(Tal tal) {
		Long talId = tal.getId();
		String talName = tal.getName();
		// Check if there's already a pending execution
		if (executionMap.containsKey(talId)) {
			// Return the execution status
			Command current = executionMap.get(talId);
			if (current.isAlive()) {
				return ExecutionStatus.RUNNING;
			}
		}
		String talLocation = Paths.get(ApiConfiguration.getTalsLocation(), talName.concat(".tal")).toString();
		List<String> commandWArgs = new ArrayList<>();
		commandWArgs.add(ApiConfiguration.getValidatorCommand());
		commandWArgs.add(MessageFormat.format(ApiConfiguration.getValidatorArgSyncTal(), talLocation));
		String argOpts = ApiConfiguration.getValidatorArgSyncTalOpts();
		if (argOpts != null) {
			commandWArgs.add(argOpts);
		}
		Command cmd = Util.createAndExecCommand(commandWArgs);
		executionMap.put(talId, cmd);
		if (cmd.getExitStatus() == Command.COMMAND_FAILED) {
			return ExecutionStatus.REQUEST_ERROR;
		}
		return ExecutionStatus.REQUESTED;
	}
}

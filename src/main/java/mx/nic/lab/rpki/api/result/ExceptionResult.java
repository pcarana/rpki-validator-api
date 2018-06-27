package mx.nic.lab.rpki.api.result;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mx.nic.lab.rpki.db.exception.http.HttpException;
import mx.nic.lab.rpki.db.pojo.CustomException;

/**
 * A result from a exception generated in a request, extends from
 * {@link ApiSingleResult}
 */
public class ExceptionResult extends ApiSingleResult {

	private final static Logger logger = Logger.getLogger(ExceptionResult.class.getName());

	/**
	 * Empty constructor
	 */
	public ExceptionResult() {
		super();
	}

	/**
	 * Build based on a {@link HttpException}
	 * 
	 * @param e
	 */
	public ExceptionResult(HttpException e) {
		this();
		CustomException customException = new CustomException();
		customException.setErrorCode(e.getHttpResponseStatusCode());
		customException.setErrorTitle(e.getMessage());

		setApiObject(customException);
	}

	/**
	 * Read the request and fill the error data
	 * 
	 * @param httpRequest
	 */
	public ExceptionResult(HttpServletRequest httpRequest) {
		this();
		CustomException customException = new CustomException();
		setApiObject(customException);

		String errorTitle = null;
		String errorDescription;

		Object objectCode = httpRequest.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		if (objectCode != null && objectCode instanceof Integer) {
			// According to DOCs, always an Integer
			customException.setErrorCode((Integer) objectCode);
		} else {
			customException.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			customException.setErrorDescription("Unknown status_code: " + objectCode);
			return;
		}

		boolean logWarning = true;
		Object objectMessage = httpRequest.getAttribute(RequestDispatcher.ERROR_MESSAGE);
		String localMessage = objectMessage != null ? objectMessage.toString() : null;
		customException.setErrorDescription(localMessage);
		switch (customException.getErrorCode()) {
		case 400:
			errorTitle = "Bad request";
			errorDescription = localMessage;
			break;
		case 401:
			errorTitle = "Forbidden request";
			errorDescription = "Must log in to process the request";
			break;
		case 404:
			errorTitle = "Object not found";
			errorDescription = localMessage;
			break;
		case 422:
			errorTitle = "Unprocessable HTTP Entity";
			errorDescription = localMessage;
			break;
		case 500:
			errorTitle = "Internal server error";
			errorDescription = localMessage;
			// The error wasn't "manually" sent, a.k.a is really unexpected
			Object errorException = httpRequest.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
			if (errorException != null) {
				Throwable throwable = (Throwable) errorException;
				logger.log(Level.SEVERE, throwable.getMessage(), throwable);
				logWarning = false;
			}
			break;
		default:
			// At least get the description, if there's one
			errorDescription = localMessage;
			break;
		}
		if (logWarning) {
			logger.log(Level.WARNING, "Returned code " + customException.getErrorCode() + ": " + errorDescription);
		}
		customException.setErrorTitle(errorTitle);
		customException.setErrorDescription(errorDescription);
	}

}

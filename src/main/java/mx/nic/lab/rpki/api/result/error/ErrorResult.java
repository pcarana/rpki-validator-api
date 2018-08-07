package mx.nic.lab.rpki.api.result.error;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.exception.ValidationError;
import mx.nic.lab.rpki.db.exception.ValidationException;

/**
 * An error result that may come from an exception generated in a request or by
 * the DA implementation, extends from {@link ApiResult}
 */
public class ErrorResult extends ApiResult {

	private static final Logger logger = Logger.getLogger(ErrorResult.class.getName());

	/**
	 * General message of the error
	 */
	private String message;

	/**
	 * List of errors with more detail
	 */
	private List<ErrorData> errors;

	/**
	 * Empty constructor
	 */
	public ErrorResult() {
		this.errors = new ArrayList<ErrorData>();
	}

	/**
	 * Build an instance receiving only the code
	 * 
	 * @param code
	 */
	public ErrorResult(int code) {
		this.code = code;
		this.errors = new ArrayList<ErrorData>();
	}

	/**
	 * Build an instance receiving code and message
	 * 
	 * @param code
	 * @param message
	 */
	public ErrorResult(int code, String message) {
		this.code = code;
		this.message = message;
		this.errors = new ArrayList<ErrorData>();
	}

	/**
	 * Build based on a {@link HttpException}
	 * 
	 * @param e
	 */
	public ErrorResult(HttpException e) {
		this.code = e.getHttpResponseStatusCode();
		this.message = e.getMessage();
		this.errors = new ArrayList<ErrorData>();
	}

	/**
	 * Build based on a {@link ValidationException}, iterates over the
	 * {@link ValidationException#getValidationErrors()} to get the corresponding
	 * error label
	 * 
	 * @param e
	 */
	public ErrorResult(ValidationException e) {
		this.code = HttpServletResponse.SC_BAD_REQUEST;
		this.message = "#{error.multipleErrors}";
		this.errors = new ArrayList<ErrorData>();
		for (ValidationError ver : e.getValidationErrors()) {
			String propertyKey = ver.getObjectName();
			String field = ver.getField();
			String description = null;
			if (field != null) {
				propertyKey = propertyKey.concat(".").concat(field);
			}
			// Get the message according to the errorType
			switch (ver.getErrorType()) {
			case NULL:
				description = "#{error.validation.null}";
				break;
			case NOT_NULL:
				description = "#{error.validation.notNull}";
				break;
			case UNEXPECTED_TYPE:
				description = "#{error.validation.unexpectedType}";
				break;
			case UNEXPECTED_VALUE:
				description = "#{error.validation.unexpectedValue}";
				break;
			case VALUE_OUT_OF_RANGE:
				description = Util.concatenateParamsToLabel("#{error.validation.valueOutOfRange}", ver.getMin(),
						ver.getMax());
				break;
			case LENGTH_OUT_OF_RANGE:
				description = Util.concatenateParamsToLabel("#{error.validation.lengthOutOfRange}", ver.getMin(),
						ver.getMax());
				break;
			case OBJECT_EXISTS:
				description = "#{error.validation.objectExists}";
				break;
			case OBJECT_NOT_EXISTS:
				description = "#{error.validation.objectNotExists}";
				break;
			default:
				description = "";
				break;
			}
			this.errors.add(new ErrorData(propertyKey, description));
		}
	}

	/**
	 * Read the request and fill the error data
	 * 
	 * @param httpRequest
	 */
	public ErrorResult(HttpServletRequest httpRequest) {
		this.errors = new ArrayList<ErrorData>();
		Object objectCode = httpRequest.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		if (objectCode != null && objectCode instanceof Integer) {
			// According to DOCs, always an Integer
			this.code = (Integer) objectCode;
		} else {
			this.code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			this.message = Util.concatenateParamsToLabel("#{error.unknownStatusCode}", objectCode.toString());
			return;
		}

		boolean logWarning = true;
		String errorMessage = null;
		Object objectMessage = httpRequest.getAttribute(RequestDispatcher.ERROR_MESSAGE);
		String localMessage = objectMessage != null ? objectMessage.toString() : "";
		switch (getCode()) {
		case HttpServletResponse.SC_BAD_REQUEST:
			errorMessage = "#{error.badRequest}";
			break;
		case HttpServletResponse.SC_UNAUTHORIZED:
			errorMessage = "#{error.unauthorized}";
			break;
		case HttpServletResponse.SC_NOT_FOUND:
			errorMessage = Util.concatenateParamsToLabel("#{error.notFound}", localMessage);
			break;
		case HttpServletResponse.SC_INTERNAL_SERVER_ERROR:
			errorMessage = "#{error.internalError}";
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
			errorMessage = localMessage;
			break;
		}
		if (logWarning) {
			logger.log(Level.WARNING, "Returned code " + getCode() + ": " + errorMessage);
		}
		this.message = errorMessage;
	}

	@Override
	public JsonStructure toJsonStructure() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addKeyValueToBuilder(builder, "code", getCode(), false);
		addKeyValueToBuilder(builder, "message", getMessage(), true);
		if (getErrors() != null && !getErrors().isEmpty()) {
			JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
			for (ErrorData errData : getErrors()) {
				JsonObjectBuilder errBuilder = Json.createObjectBuilder();
				addKeyValueToBuilder(errBuilder, "title", errData.getTitle(), true);
				addKeyValueToBuilder(errBuilder, "description", errData.getDescription(), true);
				arrayBuilder.add(errBuilder);
			}
			addKeyValueToBuilder(builder, "errors", arrayBuilder, true);
		}
		return builder.build();
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<ErrorData> getErrors() {
		return errors;
	}

	public void setErrors(List<ErrorData> errors) {
		this.errors = errors;
	}

}

package mx.nic.lab.rpki.api.validation;

import java.io.File;
import java.time.Duration;
import java.time.format.DateTimeParseException;

import mx.nic.lab.rpki.api.config.ApiConfiguration;
import mx.nic.lab.rpki.db.exception.InitializationException;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.RpkiObjectDAO;
import mx.nic.lab.rpki.db.spi.RpkiRepositoryDAO;
import mx.nic.lab.rpki.db.spi.TalDAO;
import mx.nic.lab.rpki.db.spi.ValidationRunDAO;

/**
 * Class that all the validation related services should inherit to get access
 * to the needed DAOs
 *
 */
public class ValidationService {

	private static RpkiRepositoryDAO rpkiRepositoryDAO;
	private static RpkiObjectDAO rpkiObjectDAO;
	private static TalDAO talDAO;
	private static ValidationRunDAO validationRunDAO;

	private static Duration rpkiObjectCleanupGrace;
	private static Duration rsyncRepositoryDownloadInterval;
	private static File localRsyncStorageDirectory;
	private static File talsLocation;

	/**
	 * Validate that the required DAOs and properties are set
	 * 
	 * @throws InitializationException
	 */
	public static void init() throws InitializationException {
		// Check for the required DAOs
		rpkiRepositoryDAO = DataAccessService.getRpkiRepositoryDAO();
		if (rpkiRepositoryDAO == null) {
			throw new InitializationException(
					"The RpkiRepositoryDAO implementation is necessary to run the validation services");
		}
		rpkiObjectDAO = DataAccessService.getRpkiObjectDAO();
		if (rpkiObjectDAO == null) {
			throw new InitializationException(
					"The RpkiObjectDAO implementation is necessary to run the validation services");
		}
		talDAO = DataAccessService.getTalDAO();
		if (talDAO == null) {
			throw new InitializationException("The TalDAO implementation is necessary to run the validation services");
		}
		validationRunDAO = DataAccessService.getValidationRunDAO();
		if (validationRunDAO == null) {
			throw new InitializationException(
					"The ValidationRunDAO implementation is necessary to run the validation services");
		}
		// And set the properties
		try {
			rpkiObjectCleanupGrace = Duration.parse(ApiConfiguration.getRpkiObjectCleanupInterval());
		} catch (DateTimeParseException e) {
			throw new InitializationException("Error parsing the rpkiObjectCleanupGrace", e);
		}
		try {
			rsyncRepositoryDownloadInterval = Duration.parse(ApiConfiguration.getRsyncDownloadInterval());
		} catch (DateTimeParseException e) {
			throw new InitializationException("Error parsing the rsyncRepositoryDownloadInterval", e);
		}
		localRsyncStorageDirectory = new File(ApiConfiguration.getDownloadedRepositoriesLocation());
		talsLocation = new File(ApiConfiguration.getTalsLocation());
	}

	public static RpkiRepositoryDAO getRpkiRepositoryDAO() {
		return rpkiRepositoryDAO;
	}

	public static RpkiObjectDAO getRpkiObjectDAO() {
		return rpkiObjectDAO;
	}

	public static TalDAO getTalDAO() {
		return talDAO;
	}

	public static ValidationRunDAO getValidationRunDAO() {
		return validationRunDAO;
	}

	public static Duration getRpkiObjectCleanupGrace() {
		return rpkiObjectCleanupGrace;
	}

	public static Duration getRsyncRepositoryDownloadInterval() {
		return rsyncRepositoryDownloadInterval;
	}

	public static File getLocalRsyncStorageDirectory() {
		return localRsyncStorageDirectory;
	}

	public static File getTalsLocation() {
		return talsLocation;
	}
}

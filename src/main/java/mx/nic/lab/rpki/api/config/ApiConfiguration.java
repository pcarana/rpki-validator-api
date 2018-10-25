package mx.nic.lab.rpki.api.config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.quartz.CronExpression;

import mx.nic.lab.rpki.db.exception.InitializationException;

/**
 * Main configuration for the API
 *
 */
public class ApiConfiguration {

	private static Properties systemProperties;

	// Configuration keys
	private static final String LANGUAGE_KEY = "language";
	private static final String MAX_RESPONSE_RESULTS_KEY = "max.response.results";
	private static final String DOWNLOADED_REPO_LOCATION_KEY = "downloaded.repositories.location";
	private static final String TALS_LOCATION_KEY = "tals.location";
	private static final String TRUST_ANCHOR_VALIDATION_SCHEDULE_KEY = "trust.anchor.validation.schedule";
	private static final String RPKI_OBJECT_CLEANUP_INTERVAL_KEY = "rpki.object.cleanup.grace.duration";

	// Properties to configure
	private static String serverLanguage;
	private static Integer maxResponseResults;
	private static String downloadedRepositoriesLocation;
	private static String talsLocation;
	private static String trustAnchorValidationSchedule;
	private static String rpkiObjectCleanupInterval;

	private ApiConfiguration() {
		// No code
	}

	/**
	 * Initialize the API using the configuration received
	 * 
	 * @param config
	 * @throws InitializationException
	 *             If there's an error loading the properties
	 */
	public static void initialize(Properties config) throws InitializationException {
		ApiConfiguration.systemProperties = config;
		init();
	}

	private static void init() throws InitializationException {
		List<String> invalidProperties = new ArrayList<>();
		List<Exception> exceptions = new ArrayList<>();

		if (isPropertyNullOrEmpty(LANGUAGE_KEY)) {
			invalidProperties.add(LANGUAGE_KEY);
		} else {
			serverLanguage = systemProperties.getProperty(LANGUAGE_KEY).trim();
			Locale validLocale = new Locale(serverLanguage);
			List<Locale> availableLocales = Arrays.asList(Locale.getAvailableLocales());
			if (!availableLocales.contains(validLocale)) {
				invalidProperties.add(LANGUAGE_KEY);
				exceptions.add(new IllegalArgumentException("The language " + serverLanguage + " couldn't be loaded"));
			} else {
				Locale.setDefault(validLocale);
			}
		}

		if (isPropertyNullOrEmpty(MAX_RESPONSE_RESULTS_KEY)) {
			invalidProperties.add(MAX_RESPONSE_RESULTS_KEY);
		} else {
			try {
				maxResponseResults = Integer.parseInt(systemProperties.getProperty(MAX_RESPONSE_RESULTS_KEY).trim());
			} catch (NumberFormatException e) {
				invalidProperties.add(MAX_RESPONSE_RESULTS_KEY);
				exceptions.add(e);
			}
		}

		if (isPropertyNullOrEmpty(DOWNLOADED_REPO_LOCATION_KEY)) {
			invalidProperties.add(DOWNLOADED_REPO_LOCATION_KEY);
		} else {
			downloadedRepositoriesLocation = systemProperties.getProperty(DOWNLOADED_REPO_LOCATION_KEY).trim();
			if (!isValidLocation(downloadedRepositoriesLocation, exceptions)) {
				invalidProperties.add(DOWNLOADED_REPO_LOCATION_KEY);
			}
		}

		if (isPropertyNullOrEmpty(TALS_LOCATION_KEY)) {
			invalidProperties.add(TALS_LOCATION_KEY);
		} else {
			talsLocation = systemProperties.getProperty(TALS_LOCATION_KEY).trim();
			if (!isValidLocation(talsLocation, exceptions)) {
				invalidProperties.add(TALS_LOCATION_KEY);
			}
		}

		if (isPropertyNullOrEmpty(TRUST_ANCHOR_VALIDATION_SCHEDULE_KEY)) {
			invalidProperties.add(TRUST_ANCHOR_VALIDATION_SCHEDULE_KEY);
		} else {
			trustAnchorValidationSchedule = systemProperties.getProperty(TRUST_ANCHOR_VALIDATION_SCHEDULE_KEY).trim();
			if (!isValidCronExpression(trustAnchorValidationSchedule, exceptions)) {
				invalidProperties.add(TRUST_ANCHOR_VALIDATION_SCHEDULE_KEY);
			}
		}

		if (isPropertyNullOrEmpty(RPKI_OBJECT_CLEANUP_INTERVAL_KEY)) {
			invalidProperties.add(RPKI_OBJECT_CLEANUP_INTERVAL_KEY);
		} else {
			rpkiObjectCleanupInterval = systemProperties.getProperty(RPKI_OBJECT_CLEANUP_INTERVAL_KEY).trim();
			if (!isValidDuration(rpkiObjectCleanupInterval, exceptions)) {
				invalidProperties.add(RPKI_OBJECT_CLEANUP_INTERVAL_KEY);
			}
		}

		if (!invalidProperties.isEmpty()) {
			InitializationException invalidValueException = new InitializationException(
					"The following required properties were not found or have invalid values in configuration file : "
							+ invalidProperties.toString());
			for (Exception exception : exceptions) {
				invalidValueException.addSuppressed(exception);
			}
			throw invalidValueException;
		}
	}

	/**
	 * Check if the property is null or empty
	 * 
	 * @param propertyKey
	 *            Key of the property validated
	 * @return <code>boolean</code> indicating if the property is null or empty
	 */
	private static boolean isPropertyNullOrEmpty(String propertyKey) {
		String systemProperty = systemProperties.getProperty(propertyKey);
		return systemProperty == null || systemProperty.trim().isEmpty();
	}

	/**
	 * Validate that the <code>location</code> is a valid path and a directory
	 * 
	 * @param location
	 * @param exceptions
	 * @return
	 */
	private static boolean isValidLocation(String location, List<Exception> exceptions) {
		try {
			Path validPath = Paths.get(location);
			File validFile = validPath.toFile();
			if (validFile.exists() && validFile.isDirectory()) {
				return true;
			}
			exceptions.add(
					new IllegalArgumentException("The location \"" + location + "\" isn't valid or isn't a directory"));
		} catch (Exception e) {
			exceptions.add(e);
		}
		return false;
	}

	/**
	 * Validate the <code>value</code> as a {@link Duration} using
	 * {@link Duration#parse(CharSequence)}
	 * 
	 * @param value
	 * @param exceptions
	 * @return
	 */
	private static boolean isValidDuration(String value, List<Exception> exceptions) {
		try {
			Duration.parse(value);
			return true;
		} catch (DateTimeParseException e) {
			exceptions.add(new IllegalArgumentException("The interval " + value + " isn't valid"));
		}
		return false;
	}

	/**
	 * Validate the <code>value</code> as a CRON expression using
	 * {@link CronExpression#validateExpression(String)}
	 * 
	 * @param value
	 * @param exceptions
	 * @return
	 */
	private static boolean isValidCronExpression(String value, List<Exception> exceptions) {
		try {
			CronExpression.validateExpression(value);
			return true;
		} catch (ParseException e) {
			exceptions.add(e);
		}
		return false;
	}

	public static String getServerLanguage() {
		return serverLanguage;
	}

	public static Integer getMaxResponseResults() {
		return maxResponseResults;
	}

	public static String getDownloadedRepositoriesLocation() {
		return downloadedRepositoriesLocation;
	}

	public static String getTalsLocation() {
		return talsLocation;
	}

	public static String getTrustAnchorValidationSchedule() {
		return trustAnchorValidationSchedule;
	}

	public static String getRpkiObjectCleanupInterval() {
		return rpkiObjectCleanupInterval;
	}
}

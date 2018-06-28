package mx.nic.lab.rpki.api.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import mx.nic.lab.rpki.db.exception.InitializationException;

/**
 * Main configuration for the API
 *
 */
public class ApiConfiguration {

	private static Properties systemProperties;

	// Configuration keys
	private static final String LANGUAGE_KEY = "language";

	// Properties to configure
	// FIXME probably this won't be used, since the locale is set to the Context
	private static String serverLanguage;

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

		if (!invalidProperties.isEmpty()) {
			InitializationException invalidValueException = new InitializationException(
					"The following required properties were not found or are invalid values in configuration file : "
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

	public static String getServerLanguage() {
		return serverLanguage;
	}

	public static void setServerLanguage(String serverLanguage) {
		ApiConfiguration.serverLanguage = serverLanguage;
	}
}

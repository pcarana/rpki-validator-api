package mx.nic.lab.rpki.api.listener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.LogManager;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import mx.nic.lab.rpki.api.config.ApiConfiguration;
import mx.nic.lab.rpki.api.validation.MasterScheduler;
import mx.nic.lab.rpki.db.exception.InitializationException;
import mx.nic.lab.rpki.db.service.DataAccessService;

@WebListener
public class ApiInitializer implements ServletContextListener {

	/**
	 * File name from where the logging properties will be loaded
	 */
	private static final String LOGGING_CONFIGURATION_FILE = "logging";

	/**
	 * File name from where the API main properties will be loaded
	 */
	private static final String CONFIGURATION_FILE = "configuration";

	/**
	 * File name from where the configuration of the data access implementation will
	 * be loaded.
	 */
	private static final String DATA_ACCESS_FILE = "data-access";

	/**
	 * Name of the init parameter whose value is the location of user's logging
	 * configuration to overwrite the default config
	 */
	private static final String USER_LOGGING_CONFIGURATION_PARAM_NAME = "loggingConfigurationUserPath";

	/**
	 * Name of the init parameter whose value is the location of user's
	 * configuration to overwrite the default config
	 */
	private static final String USER_CONFIGURATION_PARAM_NAME = "configurationUserPath";

	/**
	 * Name of the init parameter whose value is the location of user's data access
	 * configuration to overwrite the default config
	 */
	private static final String USER_DATA_ACCESS_PARAM_NAME = "dataAccessUserPath";

	private static ServletContext servletContext;

	@Override
	public void contextInitialized(ServletContextEvent event) {
		servletContext = event.getServletContext();
		// Load logs configuration
		try {
			initLogging(LOGGING_CONFIGURATION_FILE, USER_LOGGING_CONFIGURATION_PARAM_NAME);
		} catch (IOException | InitializationException e) {
			throw new IllegalArgumentException(e);
		}
		// Load app configuration
		try {
			Properties appConfig = loadConfig(CONFIGURATION_FILE, USER_CONFIGURATION_PARAM_NAME);
			ApiConfiguration.initialize(appConfig);
		} catch (IOException | InitializationException e) {
			throw new IllegalArgumentException(e);
		}
		// Load DA configuration
		try {
			Properties dataAccessConfig = loadConfig(DATA_ACCESS_FILE, USER_DATA_ACCESS_PARAM_NAME);
			DataAccessService.initialize(dataAccessConfig);
		} catch (IOException | InitializationException e) {
			throw new IllegalArgumentException(e);
		}
		// Everything is ok for now, run initial validations
		try {
			MasterScheduler.initSchedule();
		} catch (InitializationException e) {
			throw new IllegalArgumentException(e);
		}

	}

	private Properties loadConfig(String baseFileName, String userPathParamName) throws IOException {
		// First, load the default values (from META-INF)
		String fileName = "META-INF/" + baseFileName + ".properties";
		Properties properties = new Properties();
		try (InputStream configStream = ApiInitializer.class.getClassLoader().getResourceAsStream(fileName)) {
			if (configStream != null) {
				properties.load(configStream);
			}
		}

		// Then, override with whatever the user set up.
		String userFilePath = servletContext.getInitParameter(userPathParamName);
		if (userFilePath == null) {
			userFilePath = "WEB-INF/" + baseFileName + ".properties";
		} else {
			Path path = Paths.get(userFilePath, baseFileName + ".properties");
			userFilePath = path.toString();
		}
		try (InputStream inStream = servletContext.getResourceAsStream(userFilePath);) {
			if (inStream != null) {
				properties.load(inStream);
			}
		}

		return properties;
	}

	private void initLogging(String baseFileName, String userPathParamName)
			throws IOException, InitializationException {
		// First check if the user set up the logging config
		String userFilePath = servletContext.getInitParameter(userPathParamName);
		if (userFilePath == null) {
			userFilePath = "WEB-INF/" + baseFileName + ".properties";
		} else {
			Path path = Paths.get(userFilePath, baseFileName + ".properties");
			userFilePath = path.toString();
		}
		try (InputStream inStream = servletContext.getResourceAsStream(userFilePath);) {
			if (inStream != null) {
				LogManager.getLogManager().readConfiguration(inStream);
				System.setProperty("java.util.logging.config.file", userFilePath);
				return;
			}
		}

		// Now load default properties (the user didn't set up config)
		String fileName = "META-INF/" + baseFileName + ".properties";
		try (InputStream inStream = ApiInitializer.class.getClassLoader().getResourceAsStream(fileName)) {
			if (inStream != null) {
				LogManager.getLogManager().readConfiguration(inStream);
				System.setProperty("java.util.logging.config.file", fileName);
			}
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		servletContext = null;
		MasterScheduler.shutdown();
	}

	public static ServletContext getServletContext() {
		return servletContext;
	}

}

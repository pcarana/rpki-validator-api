package mx.nic.lab.rpki.api.listener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

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
	 * File name from where the API main properties will be loaded
	 */
	private static final String CONFIGURATION_FILE = "configuration";

	/**
	 * File name from where the configuration of the data access implementation will
	 * be loaded.
	 */
	private static final String DATA_ACCESS_FILE = "data-access";

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

	/**
	 * Servlet context used by the application
	 */
	private static ServletContext servletContext;

	@Override
	public void contextInitialized(ServletContextEvent event) {
		servletContext = event.getServletContext();
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
		// First, load the default values (from app resources)
		String fileName = baseFileName + ".properties";
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

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		MasterScheduler.shutdown();
		DataAccessService.terminate();
		servletContext = null;
	}

	public static ServletContext getServletContext() {
		return servletContext;
	}

}

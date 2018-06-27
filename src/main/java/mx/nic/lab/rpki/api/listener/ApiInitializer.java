package mx.nic.lab.rpki.api.listener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import mx.nic.lab.rpki.db.exception.InitializationException;
import mx.nic.lab.rpki.db.service.DataAccessService;

@WebListener
public class ApiInitializer implements ServletContextListener {

	/**
	 * File from which we will load the configuration of the data access
	 * implementation.
	 */
	private static final String DATA_ACCESS_FILE = "data-access";

	private static ServletContext servletContext;

	@Override
	public void contextInitialized(ServletContextEvent event) {
		servletContext = event.getServletContext();
		Properties serverConfig = new Properties();
		try {
			Properties dataAccessConfig = loadConfig(DATA_ACCESS_FILE);
			for (Entry<Object, Object> entry : dataAccessConfig.entrySet()) {
				serverConfig.put(entry.getKey(), entry.getValue());
			}
			DataAccessService.initialize(serverConfig);
		} catch (IOException | InitializationException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private Properties loadConfig(String baseFileName) throws IOException {
		// FIXME First, load the default values (from META-INF).
		Properties p = new Properties();
		String path = "WEB-INF/" + baseFileName + ".properties";
		try (InputStream inStream = servletContext.getResourceAsStream(path);) {
			if (inStream != null) {
				p.load(inStream);
			}
		}

		return p;
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		servletContext = null;
	}

	public static ServletContext getServletContext() {
		return servletContext;
	}

}

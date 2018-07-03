package mx.nic.lab.rpki.sqlite.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.sqlite.SQLiteDataSource;

import mx.nic.lab.rpki.db.exception.InitializationException;

/**
 * Instance to handle Database session from a SQLite DB
 *
 */
public class DatabaseSession {

	private static DataSource dataSource;

	private static final Logger logger = Logger.getLogger(DatabaseSession.class.getName());

	/**
	 * Initialize the DB connection based on the configuration provided
	 * 
	 * @param config
	 * @throws InitializationException
	 */
	public static void initConnection(Properties config) throws InitializationException {
		/*
		 * At compile time, the application does not know the data access
		 * implementation, and the data access implementation does not know the
		 * application.
		 * 
		 * So we don't know for sure where the data source can be found, and the
		 * application cannot tell us. There is no interface for the application to
		 * provide us with a data source, because the data source is OUR problem. From
		 * the app's perspective, it might not even exist.
		 * 
		 * So what we're going to do is probe the candidate locations and stick with
		 * what works.
		 */
		dataSource = findDataSource(config);
		if (dataSource != null) {
			logger.info("Data source found.");
			return;
		}
		logger.info("I could not find the API data source in the context. "
				+ "This won't be a problem if I can find it in the configuration. Attempting that now... ");
		dataSource = loadDataSourceFromProperties(config);
	}

	private static DataSource findDataSource(Properties config) {
		Context context;
		try {
			context = new InitialContext();
		} catch (NamingException e) {
			logger.log(Level.INFO, "I could not instance an initial context. "
					+ "I will not be able to find the data source by JDNI name.", e);
			return null;
		}

		String jdniName = config.getProperty("db_resource_name");
		if (jdniName != null) {
			return findDataSource(context, jdniName);
		}

		// Try the default string.
		// In some server containers (such as Wildfly), the default string is
		// "java:/comp/env/jdbc/rpki-api".
		// In other server containers (such as Payara), the string is
		// "java:comp/env/jdbc/rpki-api".
		// In other server containers (such as Tomcat), it doesn't matter.
		DataSource result = findDataSource(context, "java:/comp/env/jdbc/rpki-api");
		if (result != null) {
			return result;
		}

		return findDataSource(context, "java:comp/env/jdbc/rpki-api");
	}

	private static DataSource findDataSource(Context context, String jdniName) {
		logger.info("Attempting to find data source '" + jdniName + "'...");
		try {
			return (DataSource) context.lookup(jdniName);
		} catch (NamingException e) {
			logger.info("Data source not found. Attempting something else...");
			return null;
		}
	}

	private static DataSource loadDataSourceFromProperties(Properties config) throws InitializationException {
		String driverClassName = config.getProperty("driverClassName");
		String url = config.getProperty("url");
		if (driverClassName == null || url == null) {
			throw new InitializationException("I can't find a data source in the configuration.");
		}

		try {
			Class.forName(driverClassName);
		} catch (ClassNotFoundException e) {
			throw new InitializationException("Driver not found: " + driverClassName);
		}

		SQLiteDataSource sqliteDataSource = new SQLiteDataSource();
		sqliteDataSource.setUrl(url);

		// Load the test query, if not present then load the most common
		// (http://stackoverflow.com/questions/3668506)
		String testQuery = config.getProperty("testQuery", "select 1");
		try {
			testDatabase(sqliteDataSource, testQuery);
		} catch (SQLException e) {
			throw new InitializationException("The database connection test yielded failure.", e);
		}

		return sqliteDataSource;
	}

	private static void testDatabase(SQLiteDataSource ds, String testQuery) throws SQLException {
		try (Connection connection = ds.getConnection(); Statement statement = connection.createStatement();) {
			logger.log(Level.INFO, "Executing QUERY: " + testQuery);
			ResultSet resultSet = statement.executeQuery(testQuery);

			if (!resultSet.next()) {
				throw new SQLException("'" + testQuery + "' returned no rows.");
			}
			int result = resultSet.getInt(1);
			if (result != 1) {
				throw new SQLException("'" + testQuery + "' returned " + result);
			}
		}
	}

	/**
	 * Get the connection from the loaded DataSource
	 * 
	 * @return A {@link Connection} from the {@link DataSource}
	 * @throws SQLException
	 */
	public static Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}
}

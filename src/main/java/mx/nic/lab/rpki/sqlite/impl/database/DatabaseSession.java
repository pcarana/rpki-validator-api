package mx.nic.lab.rpki.sqlite.impl.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		dataSource = loadDataSourceFromProperties(config);
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

package mx.nic.lab.rpki.sqlite.model;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import mx.nic.lab.rpki.db.pojo.Roa;
import mx.nic.lab.rpki.sqlite.database.QueryGroup;

/**
 * Model to retrieve ROA data from the database
 *
 */
public class RoaModel {

	private final static Logger logger = Logger.getLogger(RoaModel.class.getName());

	/**
	 * Query group ID, it MUST be the same that the .sql file where the queries are
	 * found
	 */
	private final static String QUERY_GROUP = "Roa";

	private static QueryGroup queryGroup = null;

	// Queries IDs used by this model
	private static final String GET_BY_ID = "getById";

	/**
	 * Loads the queries corresponding to this model, based on the QUERY_GROUP
	 * constant
	 * 
	 * @param schema
	 */
	public static void loadQueryGroup(String schema) {
		try {
			QueryGroup group = new QueryGroup(QUERY_GROUP, schema);
			setQueryGroup(group);
		} catch (IOException e) {
			throw new RuntimeException("Error loading query group", e);
		}
	}

	// FIXME Complete javadoc when the implementation is complete
	public static Roa getById(Long id, Connection connection) throws SQLException {
		String query = getQueryGroup().getQuery(GET_BY_ID);
		try (PreparedStatement statement = connection.prepareStatement(query)) {
			statement.setLong(1, id);
			logger.log(Level.INFO, "Executing QUERY: " + statement.toString());
			ResultSet rs = statement.executeQuery();
			if (!rs.next()) {
				return null;
			}
			Roa roa = new Roa();
			do {
				roa.setId(rs.getLong("id"));
			} while (rs.next());

			return roa;
		}
	}

	public static QueryGroup getQueryGroup() {
		return queryGroup;
	}

	public static void setQueryGroup(QueryGroup queryGroup) {
		RoaModel.queryGroup = queryGroup;
	}
}

package mx.nic.lab.rpki.sqlite.model;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.sqlite.database.QueryGroup;
import mx.nic.lab.rpki.sqlite.object.TalDbObject;

/**
 * Model to retrieve TALs data from the database
 *
 */
public class TalModel {

	private final static Logger logger = Logger.getLogger(TalModel.class.getName());

	/**
	 * Query group ID, it MUST be the same that the .sql file where the queries are
	 * found
	 */
	private final static String QUERY_GROUP = "Tal";

	private static QueryGroup queryGroup = null;

	// Queries IDs used by this model
	private static final String GET_BY_ID = "getById";
	private static final String GET_ALL = "getAll";

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

	/**
	 * Get a {@link Tal} by its ID, return null if no data is found
	 * 
	 * @param id
	 * @param connection
	 * @return The {@link Tal} found
	 * @throws SQLException
	 */
	public static Tal getById(Long id, Connection connection) throws SQLException {
		String query = getQueryGroup().getQuery(GET_BY_ID);
		try (PreparedStatement statement = connection.prepareStatement(query)) {
			statement.setLong(1, id);
			logger.log(Level.INFO, "Executing QUERY: " + statement.toString());
			ResultSet rs = statement.executeQuery();
			if (!rs.next()) {
				return null;
			}
			Tal tal = null;
			do {
				tal = new TalDbObject(rs);
				loadRelatedObjects(tal, connection);
			} while (rs.next());

			return tal;
		}
	}

	/**
	 * Get all the {@link Tal}s, return empty list when no files are found
	 * 
	 * @param connection
	 * @return The list of {@link Tal}s, or empty list when no data is found
	 * @throws SQLException
	 */
	public static List<Tal> getAll(Connection connection) throws SQLException {
		String query = getQueryGroup().getQuery(GET_ALL);
		try (PreparedStatement statement = connection.prepareStatement(query)) {
			logger.log(Level.INFO, "Executing QUERY: " + statement.toString());
			ResultSet rs = statement.executeQuery();
			if (!rs.next()) {
				return Collections.emptyList();
			}
			List<Tal> tals = new ArrayList<Tal>();
			do {
				TalDbObject tal = new TalDbObject(rs);
				loadRelatedObjects(tal, connection);
				tals.add(tal);
			} while (rs.next());

			return tals;
		}
	}

	/**
	 * Load all the related objects to the TAL
	 * 
	 * @param tal
	 * @param connection
	 * @throws SQLException
	 */
	private static void loadRelatedObjects(Tal tal, Connection connection) throws SQLException {
		Long talId = tal.getId();
		tal.setTalFiles(TalFileModel.getByTalId(talId, connection));
		tal.setTalUris(TalUriModel.getByTalId(talId, connection));
	}

	public static QueryGroup getQueryGroup() {
		return queryGroup;
	}

	public static void setQueryGroup(QueryGroup queryGroup) {
		TalModel.queryGroup = queryGroup;
	}
}

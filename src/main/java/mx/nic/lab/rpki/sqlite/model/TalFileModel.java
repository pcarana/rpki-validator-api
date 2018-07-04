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

import mx.nic.lab.rpki.db.pojo.TalFile;
import mx.nic.lab.rpki.sqlite.database.QueryGroup;
import mx.nic.lab.rpki.sqlite.object.TalFileDbObject;

/**
 * Model to retrieve TAL files data from the database
 *
 */
public class TalFileModel {

	private final static Logger logger = Logger.getLogger(TalFileModel.class.getName());

	/**
	 * Query group ID, it MUST be the same that the .sql file where the queries are
	 * found
	 */
	private final static String QUERY_GROUP = "TalFile";

	private static QueryGroup queryGroup = null;

	// Queries IDs used by this model
	private static final String GET_BY_TAL_ID = "getByTalId";

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
	 * Get all the {@link TalFile}s related to a TAL ID, return empty list when no
	 * files are found
	 * 
	 * @param talId
	 * @param connection
	 * @return The list of {@link TalFile}s related, or empty list when no data is
	 *         found
	 * @throws SQLException
	 */
	public static List<TalFile> getByTalId(Long talId, Connection connection) throws SQLException {
		String query = getQueryGroup().getQuery(GET_BY_TAL_ID);
		try (PreparedStatement statement = connection.prepareStatement(query)) {
			statement.setLong(1, talId);
			logger.log(Level.INFO, "Executing QUERY: " + statement.toString());
			ResultSet rs = statement.executeQuery();
			if (!rs.next()) {
				return Collections.emptyList();
			}
			List<TalFile> talFiles = new ArrayList<TalFile>();
			do {
				TalFileDbObject talFile = new TalFileDbObject(rs);
				talFiles.add(talFile);
			} while (rs.next());

			return talFiles;
		}
	}

	public static QueryGroup getQueryGroup() {
		return queryGroup;
	}

	public static void setQueryGroup(QueryGroup queryGroup) {
		TalFileModel.queryGroup = queryGroup;
	}
}

package mx.nic.lab.rpki.sqlite.impl.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import mx.nic.lab.rpki.db.pojo.Roa;

/**
 * Model to retrieve ROA data from the database
 *
 */
public class RoaModel {

	private final static Logger logger = Logger.getLogger(RoaModel.class.getName());

	public static Roa getById(Long id, Connection connection) throws SQLException {
		String query = "select :id id";
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
}

package mx.nic.lab.rpki.sqlite.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An object that can be loaded and stored in Database (if required)
 *
 */
public interface DatabaseObject {

	/**
	 * Load the object information from the <code>ResultSet</code>
	 * 
	 * @param resultSet
	 * @throws SQLException
	 */
	public void loadFromDatabase(ResultSet resultSet) throws SQLException;

	/**
	 * Set the <code>PreparedStatement</code> so that the Object instance can be
	 * stored to database
	 * 
	 * @param statement
	 * @throws SQLException
	 */
	public void storeToDatabase(PreparedStatement statement) throws SQLException;
}

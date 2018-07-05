package mx.nic.lab.rpki.sqlite.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import mx.nic.lab.rpki.db.pojo.Tal;

public class TalDbObject extends Tal implements DatabaseObject {

	public static final String ID_COLUMN = "tal_id";
	public static final String LAST_SYNC_COLUMN = "tal_last_sync";
	public static final String PUBLIC_KEY_COLUMN = "tal_public_key";
	public static final String STATUS_COLUMN = "tal_status";
	public static final String NAME_COLUMN = "tal_name";

	public TalDbObject() {
		super();
	}

	/**
	 * Create a new instance loading values from a <code>ResultSet</code>
	 * 
	 * @param resultSet
	 * @throws SQLException
	 */
	public TalDbObject(ResultSet resultSet) throws SQLException {
		super();
		loadFromDatabase(resultSet);
	}

	@Override
	public void loadFromDatabase(ResultSet resultSet) throws SQLException {
		setId(resultSet.getLong(ID_COLUMN));
		if (resultSet.wasNull()) {
			setId(null);
		}
		setLastSync(resultSet.getString(LAST_SYNC_COLUMN));
		setPublicKey(resultSet.getString(PUBLIC_KEY_COLUMN));
		setStatus(resultSet.getString(STATUS_COLUMN));
		setName(resultSet.getString(NAME_COLUMN));
	}

	@Override
	public void storeToDatabase(PreparedStatement statement) throws SQLException {
		// This object can't be stored to database
	}
}

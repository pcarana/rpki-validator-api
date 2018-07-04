package mx.nic.lab.rpki.sqlite.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import mx.nic.lab.rpki.db.pojo.TalFile;

/**
 * Extension of {@link TalFile} as a {@link DatabaseObject}
 *
 */
public class TalFileDbObject extends TalFile implements DatabaseObject {

	public static final String ID_COLUMN = "taf_id";
	public static final String TAL_ID_COLUMN = "tal_id";
	public static final String FILE_TYPE_COLUMN = "taf_file_type";
	public static final String STATUS_COLUMN = "taf_status";
	public static final String MESSAGE_COLUMN = "taf_message";
	public static final String LOCATION_COLUMN = "taf_location";

	public TalFileDbObject() {
		super();
	}

	/**
	 * Create a new instance loading values from a <code>ResultSet</code>
	 * 
	 * @param resultSet
	 * @throws SQLException
	 */
	public TalFileDbObject(ResultSet resultSet) throws SQLException {
		super();
		loadFromDatabase(resultSet);
	}

	@Override
	public void loadFromDatabase(ResultSet resultSet) throws SQLException {
		setId(resultSet.getLong(ID_COLUMN));
		if (resultSet.wasNull()) {
			setId(null);
		}
		setTalId(resultSet.getLong(TAL_ID_COLUMN));
		if (resultSet.wasNull()) {
			setTalId(null);
		}
		setFileType(resultSet.getString(FILE_TYPE_COLUMN));
		setStatus(resultSet.getString(STATUS_COLUMN));
		setMessage(resultSet.getString(MESSAGE_COLUMN));
		setLocation(resultSet.getString(LOCATION_COLUMN));
	}

	@Override
	public void storeToDatabase(PreparedStatement statement) throws SQLException {
		// This object can't be stored to database
	}
}

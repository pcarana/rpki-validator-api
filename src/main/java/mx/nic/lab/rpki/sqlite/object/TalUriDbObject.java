package mx.nic.lab.rpki.sqlite.object;

import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import mx.nic.lab.rpki.db.pojo.TalUri;

/**
 * Extension of {@link TalUri} as a {@link DatabaseObject}
 *
 */
public class TalUriDbObject extends TalUri implements DatabaseObject {

	public static final String ID_COLUMN = "tau_id";
	public static final String TAL_ID_COLUMN = "tal_id";
	public static final String VALUE_COLUMN = "tau_value";
	public static final String LOADED_CER_COLUMN = "tau_loaded_cer";
	public static final String LOADED_COLUMN = "tau_loaded";

	public TalUriDbObject() {
		super();
	}

	/**
	 * Create a new instance loading values from a <code>ResultSet</code>
	 * 
	 * @param resultSet
	 * @throws SQLException
	 */
	public TalUriDbObject(ResultSet resultSet) throws SQLException {
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
		setValue(resultSet.getString(VALUE_COLUMN));
		Blob blobValue = resultSet.getBlob(LOADED_CER_COLUMN);
		if (resultSet.wasNull()) {
			setLoadedCer(null);
		} else {
			try {
				// FIXME Proof that this really works
				CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
				InputStream in = blobValue.getBinaryStream();
				X509Certificate cert = (X509Certificate) certFactory.generateCertificate(in);
				setLoadedCer(cert);
			} catch (CertificateException e) {
				throw new SQLException("The " + LOADED_CER_COLUMN + "couldn't be loaded as a valid certificate", e);
			}
		}
		setLoaded(resultSet.getInt(LOADED_COLUMN) > 0);
		if (resultSet.wasNull()) {
			setId(null);
		}
	}

	@Override
	public void storeToDatabase(PreparedStatement statement) throws SQLException {
		// This object can't be stored to database
	}
}

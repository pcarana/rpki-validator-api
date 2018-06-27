package mx.nic.lab.rpki.sqlite.impl;

import java.util.Properties;

import mx.nic.lab.rpki.db.exception.InitializationException;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.spi.DataAccessImplementation;
import mx.nic.lab.rpki.db.spi.RoaDAO;
import mx.nic.lab.rpki.sqlite.impl.database.DatabaseSession;

/**
 * Implementation used for {@link DataAccessImplementation}
 *
 */
public class ProviderImplementation implements DataAccessImplementation {

	@Override
	public void init(Properties properties) throws InitializationException {
		DatabaseSession.initConnection(properties);
	}

	@Override
	public RoaDAO getRoaDAO() throws ApiDataAccessException {
		return new RoaDAOImpl();
	}

}

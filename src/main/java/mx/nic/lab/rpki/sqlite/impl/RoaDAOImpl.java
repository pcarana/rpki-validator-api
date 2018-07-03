package mx.nic.lab.rpki.sqlite.impl;

import java.sql.Connection;
import java.sql.SQLException;

import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.Roa;
import mx.nic.lab.rpki.db.spi.RoaDAO;
import mx.nic.lab.rpki.sqlite.database.DatabaseSession;
import mx.nic.lab.rpki.sqlite.model.RoaModel;

/**
 * Implementation to retrieve ROAs data
 *
 */
public class RoaDAOImpl implements RoaDAO {

	@Override
	public Roa getById(Long id) throws ApiDataAccessException {
		try (Connection connection = DatabaseSession.getConnection()) {
			return RoaModel.getById(id, connection);
		} catch (SQLException e) {
			throw new ApiDataAccessException(e);
		}
	}

}

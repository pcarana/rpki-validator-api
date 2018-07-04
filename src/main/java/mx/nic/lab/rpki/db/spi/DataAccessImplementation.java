package mx.nic.lab.rpki.db.spi;

import java.util.Properties;

import mx.nic.lab.rpki.db.exception.InitializationException;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.http.NotImplementedException;
import mx.nic.lab.rpki.db.service.DataAccessService;

/**
 * The interface of the main/central class of the implementation.
 * <p>
 * It's mainly just a hub that references the several implementation classes
 * that actually take care of the data access.
 */
public interface DataAccessImplementation {

	/**
	 * Can be used by the implementation to initialize itself.
	 * <p>
	 * Users of this library are not supposed to call this method. Call
	 * {@link DataAccessService#initialize(Properties)} instead.
	 * 
	 * @param properties
	 *            User-supplied configuration. The implementation defines the
	 *            properties that should be present here, according to its needs.
	 */
	public void init(Properties properties) throws InitializationException;

	/**
	 * Returns an instance of the implementation class that retrieves TALs data from
	 * whatever source the implementation is wrapping.
	 * <p>
	 * If no TAL data is ment to be returned by the implementation, this function is
	 * expected to either return <code>null</code> or throw a
	 * {@link NotImplementedException}.
	 */
	public TalDAO getTalDAO() throws ApiDataAccessException;

	/**
	 * Returns an instance of the implementation class that retrieves ROAs data from
	 * whatever source the implementation is wrapping.
	 * <p>
	 * If no ROA data is ment to be returned by the implementation, this function is
	 * expected to either return <code>null</code> or throw a
	 * {@link NotImplementedException}.
	 */
	public RoaDAO getRoaDAO() throws ApiDataAccessException;

}

package mx.nic.lab.rpki.api.slurm;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;

import mx.nic.lab.rpki.api.config.ApiConfiguration;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.InitializationException;
import mx.nic.lab.rpki.db.pojo.ListResult;
import mx.nic.lab.rpki.db.pojo.SlurmBgpsec;
import mx.nic.lab.rpki.db.pojo.SlurmPrefix;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.SlurmBgpsecDAO;
import mx.nic.lab.rpki.db.spi.SlurmDAO;
import mx.nic.lab.rpki.db.spi.SlurmPrefixDAO;

/**
 * Manager to handle the validation and synchronization between the SLURM file
 * and the SLURM at DA implementation. The SLURM is expected as RFC 8416
 * dictates.
 *
 */
public class SlurmManager {

	/**
	 * File from where the SLURM will be loaded
	 */
	private static File slurmLocationFile;

	/**
	 * Class logger
	 */
	private static final Logger logger = Logger.getLogger(SlurmManager.class.getName());

	/**
	 * Validate the configured SLURM, sync with DA implementation, and place a
	 * watcher
	 * 
	 * @throws InitializationException
	 *             if the SLURM has an error or there's another error performing an
	 *             initialization action
	 */
	public static void initSlurm() throws InitializationException {
		slurmLocationFile = new File(ApiConfiguration.getSlurmLocation());
		if (slurmLocationFile == null) {
			return;
		}
		List<Exception> exceptions = new ArrayList<>();
		// Execute synchronously on start
		loadSlurmFromFile(exceptions);
		if (!exceptions.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (Exception e : exceptions) {
				sb.append("[").append(e.getMessage()).append(", ").append(e.getCause()).append("] ");
			}
			throw new InitializationException("Invalid SLURM at " + slurmLocationFile + ". " + sb.toString());
		}
		// The file changes will be watched by another process (not the main process/thread)
	}

	/**
	 * Load the SLURM from the configured SLURM location
	 * 
	 * @param exceptions
	 *            list of exceptions where any error will be concatenated
	 */
	public static void loadSlurmFromFile(List<Exception> exceptions) {
		try (FileReader fr = new FileReader(slurmLocationFile); JsonParser parser = Json.createParser(fr)) {
			parser.next();
			JsonObject jsonObject = parser.getObject();
			if (!SlurmUtil.isValidSlurm(jsonObject, exceptions)) {
				System.out.println("INVALID SLURM");
				return;
			}
			byte[] currentChecksum = null;
			MessageDigest md = null;
			try {
				md = MessageDigest.getInstance("SHA-256");
			} catch (NoSuchAlgorithmException e) {
				exceptions.add(e);
				return;
			}
			md.update(Files.readAllBytes(slurmLocationFile.toPath().normalize()));
			currentChecksum = md.digest();
			// Compare the last checksum, if different then check against DA implementation
			// (the file has the priority)
			SlurmDAO dao = DataAccessService.getSlurmDAO();
			byte[] lastChecksum;
			try {
				lastChecksum = dao.getLastChecksum();
			} catch (ApiDataAccessException e) {
				exceptions.add(new Exception("The last checksum of the SLURM at database couldn't be fetched", e));
				return;
			}
			// Update DB (if needed)
			if (lastChecksum == null || !Arrays.equals(lastChecksum, currentChecksum)) {
				logger.log(Level.INFO, "Updating SLURM at DA implementation");
				updateSlurmDb(jsonObject, currentChecksum);
			}
		} catch (IOException e) {
			exceptions.add(new Exception("Failed to load SLURM " + slurmLocationFile + ": " + e.getMessage(), e));
		} catch (IllegalStateException e) {
			exceptions
					.add(new Exception("Invalid JSON object at SLURM " + slurmLocationFile + ": " + e.getMessage(), e));
		}
	}

	/**
	 * Update the SLRUM at the Data Access Implementation
	 * 
	 * @param newSlurm
	 * @param newChecksum
	 */
	private static void updateSlurmDb(JsonObject newSlurm, byte[] newChecksum) {
		if (!updatePrefixes(newSlurm)) {
			logger.log(Level.WARNING, "The SLURM prefixes couldn't be updated");
			return;
		}
		if (!updateBgpsecs(newSlurm)) {
			logger.log(Level.WARNING, "The SLURM BGPsecs couldn't be updated");
			return;
		}
		try {
			DataAccessService.getSlurmDAO().updateLastChecksum(newChecksum);
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "Error updating SLURM checksum", e);
		}
	}

	/**
	 * Update the prefixes (filters and assertions) based on the SLRUM received in
	 * <code>newSlurm</code>
	 * 
	 * @param newSlurm
	 * @return <code>boolean</code> to indicate success or failure
	 */
	private static boolean updatePrefixes(JsonObject newSlurm) {
		SlurmPrefixDAO slurmPrefixDao = DataAccessService.getSlurmPrefixDAO();
		Set<Long> removePrefixes = new HashSet<>();
		try {
			ListResult<SlurmPrefix> slurmPrefixesDb = slurmPrefixDao.getAll(null);
			slurmPrefixesDb.getResults().forEach((result) -> {
				removePrefixes.add(result.getId());
			});
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "Error getting all the prefixes", e);
			return false;
		}
		if (!updatePrefixesByType(newSlurm, SlurmPrefix.TYPE_FILTER, slurmPrefixDao, removePrefixes)) {
			logger.log(Level.WARNING, "Someting went wrong updating prefixes " + SlurmPrefix.TYPE_FILTER);
			return false;
		}
		if (!updatePrefixesByType(newSlurm, SlurmPrefix.TYPE_ASSERTION, slurmPrefixDao, removePrefixes)) {
			logger.log(Level.WARNING, "Someting went wrong updating prefixes " + SlurmPrefix.TYPE_ASSERTION);
			return false;
		}
		if (!removePrefixes.isEmpty()) {
			try {
				slurmPrefixDao.bulkDelete(removePrefixes);
			} catch (ApiDataAccessException e) {
				logger.log(Level.SEVERE, "Error deleting old prefixes from Data Access Implementation", e);
				return false;
			}
		}
		return true;
	}

	/**
	 * Update the prefixes according to its type (filter or assertion)
	 * 
	 * @param newSlurm
	 * @param type
	 * @param slurmPrefixDao
	 * @param removePrefixes
	 * @return <code>boolean</code> to indicate success or failure
	 */
	private static boolean updatePrefixesByType(JsonObject newSlurm, String type, SlurmPrefixDAO slurmPrefixDao,
			Set<Long> removePrefixes) {
		boolean result = true;
		String rootProperty = type.equals(SlurmPrefix.TYPE_FILTER) ? "validationOutputFilters"
				: "locallyAddedAssertions";
		String childProperty = type.equals(SlurmPrefix.TYPE_FILTER) ? "prefixFilters" : "prefixAssertions";
		JsonObject jsonRoot = newSlurm.getJsonObject(rootProperty);
		JsonArray jsonChild = jsonRoot.getJsonArray(childProperty);
		try {
			for (JsonObject jsonPrefix : jsonChild.getValuesAs(JsonObject.class)) {
				SlurmPrefix slurmPrefixFile = SlurmUtil.getAndvalidatePrefix(jsonPrefix, type);
				SlurmPrefix slurmPrefixDb = null;
				try {
					slurmPrefixDb = slurmPrefixDao.getPrefixByProperties(slurmPrefixFile.getAsn(),
							slurmPrefixFile.getStartPrefix(), slurmPrefixFile.getPrefixLength(),
							slurmPrefixFile.getPrefixMaxLength(), type);
					// The object doesn't exists, attempt to create
					if (slurmPrefixDb == null) {
						if (!slurmPrefixDao.create(slurmPrefixFile)) {
							logger.log(Level.SEVERE, "The object couldn't be created: " + slurmPrefixFile.toString());
							result = false;
						}
						continue;
					}
					// Compare the comment
					if (!slurmPrefixDb.getComment().equals(slurmPrefixFile.getComment())) {
						if (slurmPrefixDao.updateComment(slurmPrefixDb.getId(), slurmPrefixFile.getComment()) != 1) {
							logger.log(Level.SEVERE,
									"The object comment couldn't be updated: " + slurmPrefixDb.toString());
							result = false;
						}
					}
					removePrefixes.remove(slurmPrefixDb.getId());
				} catch (ApiDataAccessException e) {
					logger.log(Level.SEVERE, "Error performing an action at the Data Access Implementation", e);
					result = false;
				}
			}
		} catch (ClassCastException e) {
			logger.log(Level.SEVERE, "Error geting a prefix " + type + " as JSON object from the SLURM at "
					+ rootProperty.concat(childProperty), e);
			result = false;
		}
		return result;
	}

	/**
	 * Update the SLURM BGPsec (filters and assertions) based on the SLRUM received
	 * in <code>newSlurm</code>
	 * 
	 * @param newSlurm
	 * @return <code>boolean</code> to indicate success or failure
	 */
	private static boolean updateBgpsecs(JsonObject newSlurm) {
		SlurmBgpsecDAO slurmBgpsecDao = DataAccessService.getSlurmBgpsecDAO();
		Set<Long> removeBgpsecs = new HashSet<>();
		try {
			ListResult<SlurmBgpsec> slurmBgpsecsDb = slurmBgpsecDao.getAll(null);
			slurmBgpsecsDb.getResults().forEach((result) -> {
				removeBgpsecs.add(result.getId());
			});
		} catch (ApiDataAccessException e) {
			logger.log(Level.SEVERE, "Error getting all the BGPsecs", e);
			return false;
		}
		if (!updateBgpsecsByType(newSlurm, SlurmBgpsec.TYPE_FILTER, slurmBgpsecDao, removeBgpsecs)) {
			logger.log(Level.WARNING, "Someting went wrong updating BGPsecs " + SlurmBgpsec.TYPE_FILTER);
			return false;
		}
		if (!updateBgpsecsByType(newSlurm, SlurmBgpsec.TYPE_ASSERTION, slurmBgpsecDao, removeBgpsecs)) {
			logger.log(Level.WARNING, "Someting went wrong updating BGPsecs " + SlurmBgpsec.TYPE_ASSERTION);
			return false;
		}
		if (!removeBgpsecs.isEmpty()) {
			try {
				slurmBgpsecDao.bulkDelete(removeBgpsecs);
			} catch (ApiDataAccessException e) {
				logger.log(Level.SEVERE, "Error deleting old BGPsecs from Data Access Implementation", e);
				return false;
			}
		}
		return true;
	}

	/**
	 * Update the SLURM BGPsecs according to its type (filter or assertion)
	 * 
	 * @param newSlurm
	 * @param type
	 * @param slurmBgpsecDao
	 * @param removeBgpsecs
	 * @return <code>boolean</code> to indicate success or failure
	 */
	private static boolean updateBgpsecsByType(JsonObject newSlurm, String type, SlurmBgpsecDAO slurmBgpsecDao,
			Set<Long> removeBgpsecs) {
		boolean result = true;
		String rootProperty = type.equals(SlurmBgpsec.TYPE_FILTER) ? "validationOutputFilters"
				: "locallyAddedAssertions";
		String childProperty = type.equals(SlurmBgpsec.TYPE_FILTER) ? "bgpsecFilters" : "bgpsecAssertions";
		JsonObject jsonRoot = newSlurm.getJsonObject(rootProperty);
		JsonArray jsonChild = jsonRoot.getJsonArray(childProperty);
		try {
			for (JsonObject jsonBgpsec : jsonChild.getValuesAs(JsonObject.class)) {
				SlurmBgpsec slurmBgpsecFile = SlurmUtil.getAndvalidateBgpsec(jsonBgpsec, type);
				SlurmBgpsec slurmBgpsecDb = null;
				try {
					slurmBgpsecDb = slurmBgpsecDao.getBgpsecByProperties(slurmBgpsecFile.getAsn(),
							slurmBgpsecFile.getSki(), slurmBgpsecFile.getRouterPublicKey(), type);
					// The object doesn't exists, attempt to create
					if (slurmBgpsecDb == null) {
						if (!slurmBgpsecDao.create(slurmBgpsecFile)) {
							logger.log(Level.SEVERE, "The object couldn't be created: " + slurmBgpsecFile.toString());
							result = false;
						}
						continue;
					}
					// Compare the comment
					if (!slurmBgpsecDb.getComment().equals(slurmBgpsecFile.getComment())) {
						if (slurmBgpsecDao.updateComment(slurmBgpsecDb.getId(), slurmBgpsecFile.getComment()) != 1) {
							logger.log(Level.SEVERE,
									"The object comment couldn't be updated: " + slurmBgpsecDb.toString());
							result = false;
						}
					}
					removeBgpsecs.remove(slurmBgpsecDb.getId());
				} catch (ApiDataAccessException e) {
					logger.log(Level.SEVERE, "Error performing an action at the Data Access Implementation", e);
					result = false;
				}
			}
		} catch (ClassCastException e) {
			logger.log(Level.SEVERE, "Error geting a BGPsec " + type + " as JSON object from the SLURM at "
					+ rootProperty.concat(childProperty), e);
			result = false;
		}
		return result;
	}

	/**
	 * @return the SLURM location as a {@link File}
	 */
	public static File getSlurmLocationFile() {
		return slurmLocationFile;
	}
}

package mx.nic.lab.rpki.api.validation;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import mx.nic.lab.rpki.api.config.ApiConfiguration;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.exception.InitializationException;
import mx.nic.lab.rpki.db.pojo.Tal;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.TalDAO;

public class MasterScheduler {

	private static final Logger logger = Logger.getLogger(MasterScheduler.class.getName());

	private static Scheduler scheduler;

	private MasterScheduler() {
		// No code
	}

	/**
	 * Init the recurrent services for validation and the main scheduler
	 * 
	 * @throws InitializationException
	 *             if something goes wrong
	 */
	public static void initSchedule() throws InitializationException {
		CertificateTreeValidationService.init();
		RpkiRepositoryValidationService.init();
		TrustAnchorValidationService.init();
		try {
			scheduler = StdSchedulerFactory.getDefaultScheduler();
		} catch (SchedulerException e) {
			throw new InitializationException("The main scheduler couldn't be initialized", e);
		}
		TalDAO dao = DataAccessService.getTalDAO();
		if (dao == null) {
			logger.log(Level.SEVERE,
					"There was at least one necesary DAO whose implementation couldn't be found, exiting scheduler initialization");
			throw new InitializationException(
					"There was at least one necesary DAO whose implementation couldn't be found");
		}
		// Get all the TALs and validate them
		// TODO Check for changes (added TAL, removed, updated)
		File talsLocation = new File(ApiConfiguration.getTalsLocation());
		for (File tal : talsLocation.listFiles()) {
			TrustAnchorValidationService.addTalFromFile(tal);
		}
		List<Tal> allTals = null;
		try {
			allTals = dao.getAll(null);
		} catch (ApiDataAccessException e) {
			throw new InitializationException("Error fetching all TALs", e);
		}
		for (Tal tal : allTals) {
			try {
				JobDetail talValidationJob = QuartzTrustAnchorValidationJob.buildJob(tal);
				scheduler.scheduleJob(talValidationJob,
						TriggerBuilder.newTrigger().withSchedule(
								CronScheduleBuilder.cronSchedule(ApiConfiguration.getTrustAnchorValidationSchedule()))
								.build());
				scheduler.addJob(QuartzCertificateTreeValidationJob.buildJob(tal), true);
				logger.log(Level.INFO, "Registered TAL validation job for " + tal);
			} catch (SchedulerException e) {
				throw new InitializationException("Error registering TAL validation job for " + tal.toString(), e);
			}
		}
		try {
			scheduler.scheduleJob(QuartzRpkiRepositoryValidationJob.buildJob(),
					TriggerBuilder.newTrigger().withSchedule(
							CronScheduleBuilder.cronSchedule(ApiConfiguration.getRpkiRepositoryValidationSchedule()))
							.build());
			logger.log(Level.INFO, "Registered RPKI repository validation job");
			// Trigger repository validation
			triggerRpkiRepositoryValidation();
		} catch (SchedulerException e) {
			throw new InitializationException("Error registering RPKI repository validation job", e);
		}

		try {
			scheduler.scheduleJob(QuartzRpkiObjectCleanupJob.buildJob(),
					TriggerBuilder.newTrigger()
							.withSchedule(
									CronScheduleBuilder.cronSchedule(ApiConfiguration.getRpkiObjectCleanupSchedule()))
							.build());
			logger.log(Level.INFO, "Registered RPKI object cleanup job");
		} catch (SchedulerException e) {
			throw new InitializationException("Error registering RPKI object cleanup job", e);
		}

		try {
			scheduler.scheduleJob(QuartzValidationRunCleanupJob.buildJob(),
					TriggerBuilder.newTrigger().withSchedule(
							CronScheduleBuilder.cronSchedule(ApiConfiguration.getValidationRunCleanupSchedule()))
							.build());
			logger.log(Level.INFO, "Registered Validation run cleanup job");
		} catch (SchedulerException e) {
			throw new InitializationException("Error registering Validation run cleanup job", e);
		}

		try {
			scheduler.start();
		} catch (SchedulerException e) {
			throw new InitializationException("Error starting the scheduler", e);
		}
	}

	// TODO Isn't used, check if this will be necessary to keep it
	public void addTrustAnchor(Tal trustAnchor) {
		try {
			scheduler.scheduleJob(QuartzTrustAnchorValidationJob.buildJob(trustAnchor), TriggerBuilder.newTrigger()
					.startNow().withSchedule(SimpleScheduleBuilder.repeatMinutelyForever(10)).build());
			scheduler.addJob(QuartzCertificateTreeValidationJob.buildJob(trustAnchor), true);
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	// TODO Isn't used, check if this will be necessary to keep it
	public void removeTrustAnchor(Tal trustAnchor) {
		try {
			boolean trustAnchorValidationDeleted = scheduler
					.deleteJob(QuartzTrustAnchorValidationJob.getJobKey(trustAnchor));
			boolean certificateTreeValidationDeleted = scheduler
					.deleteJob(QuartzCertificateTreeValidationJob.getJobKey(trustAnchor));
			if (!trustAnchorValidationDeleted || !certificateTreeValidationDeleted) {
				throw new NullPointerException("validation job for trust anchor or certificate tree not found");
			}
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void triggerCertificateTreeValidation(Tal trustAnchor) {
		try {
			scheduler.triggerJob(QuartzCertificateTreeValidationJob.getJobKey(trustAnchor));
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void triggerRpkiRepositoryValidation() {
		try {
			scheduler.triggerJob(QuartzRpkiRepositoryValidationJob.getJobKey());
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void shutdown() {
		try {
			if (scheduler != null && scheduler.isStarted()) {
				scheduler.shutdown();
			}
		} catch (SchedulerException e) {
			logger.log(Level.SEVERE, "Error shuting down the scheduler", e);
		}
	}
}

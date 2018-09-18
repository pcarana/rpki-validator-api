package mx.nic.lab.rpki.api.validation;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.CronScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import mx.nic.lab.rpki.api.config.ApiConfiguration;
import mx.nic.lab.rpki.db.exception.InitializationException;

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
		ValidationService.init();
		try {
			scheduler = StdSchedulerFactory.getDefaultScheduler();
		} catch (SchedulerException e) {
			throw new InitializationException("The main scheduler couldn't be initialized", e);
		}
		// Get all the TALs and validate them
		TrustAnchorValidationService.runFirstValidation();
		// And add the TAL validation Job
		try {
			scheduler.scheduleJob(QuartzTrustAnchorValidationJob.buildJob(),
					TriggerBuilder.newTrigger().withSchedule(
							CronScheduleBuilder.cronSchedule(ApiConfiguration.getTrustAnchorValidationSchedule()))
							.build());
		} catch (SchedulerException e) {
			throw new InitializationException("Error registering TALs validation job", e);
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
			scheduler.start();
		} catch (SchedulerException e) {
			throw new InitializationException("Error starting the scheduler", e);
		}
	}

	public static void addTrustAnchorJob(Long talId) throws InitializationException {
		try {
			scheduler.addJob(QuartzCertificateTreeValidationJob.buildJob(talId), true);
			logger.log(Level.INFO, "Registered TAL validation job for ID " + talId);
		} catch (SchedulerException e) {
			throw new InitializationException("Error registering TAL validation job for " + talId, e);
		}
	}

	public static void removeTrustAnchorJob(Long talId) {
		try {
			boolean certificateTreeValidationDeleted = scheduler
					.deleteJob(QuartzCertificateTreeValidationJob.getJobKey(talId));
			if (!certificateTreeValidationDeleted) {
				throw new NullPointerException("validation job for certificate tree not found");
			}
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void triggerCertificateTreeValidation(Long talId) {
		try {
			scheduler.triggerJob(QuartzCertificateTreeValidationJob.getJobKey(talId));
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

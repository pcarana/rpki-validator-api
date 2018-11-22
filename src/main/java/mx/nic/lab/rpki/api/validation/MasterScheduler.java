package mx.nic.lab.rpki.api.validation;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.CronScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
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
		try {
			scheduler.scheduleJob(QuartzTrustAnchorValidationJob.buildJob(),
					TriggerBuilder.newTrigger()
							.withSchedule(CronScheduleBuilder
									.cronSchedule(ApiConfiguration.getTrustAnchorValidationSchedule())
									.withMisfireHandlingInstructionDoNothing())
							.build());
		} catch (SchedulerException e) {
			throw new InitializationException("Error registering TALs validation job", e);
		}

		try {
			scheduler.addJob(QuartzSlurmLoaderJob.buildJob(), true);
		} catch (SchedulerException e) {
			throw new InitializationException("Error registering SLURM validation job", e);
		}

		try {
			scheduler.scheduleJob(QuartzSlurmWatcherJob.buildJob(),
					TriggerBuilder.newTrigger().startNow().withSchedule(
							SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionIgnoreMisfires())
							.build());
		} catch (SchedulerException e) {
			throw new InitializationException("Error registering SLURM watcher job", e);
		}

		try {
			scheduler.start();
		} catch (SchedulerException e) {
			throw new InitializationException("Error starting the scheduler", e);
		}

		// Get all the TALs and validate them
		try {
			scheduler.triggerJob(QuartzTrustAnchorValidationJob.getJobKey());
		} catch (SchedulerException e) {
			throw new InitializationException("Error running the initial validation", e);
		}
	}

	/**
	 * Trigger the SLURM validation job
	 */
	public static void triggerSlurmValidation() {
		try {
			scheduler.triggerJob(QuartzSlurmLoaderJob.getJobKey());
		} catch (SchedulerException e) {
			logger.log(Level.SEVERE, "Error triggering SLURM validation job", e);
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

package mx.nic.lab.rpki.api.validation;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

import mx.nic.lab.rpki.api.slurm.SlurmManager;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.service.DataAccessService;

/**
 * {@link Job} used to add a {@link WatchService} at the SLURM directory
 * configured by the application.<br>
 * <br>
 * This Job is expected to be executed only once (at application start) to leave
 * the WatchService "listening" to SLURM file updates; whenever an update is
 * detected the corresponding action will be performed: on DELETE then delete
 * the whole SLURM at the DA, on EDIT then trigger the SLURM validation using
 * the {@link QuartzSlurmLoaderJob}.
 *
 */
@DisallowConcurrentExecution
public class QuartzSlurmWatcherJob implements Job {

	/**
	 * {@link WatchService} to monitor changes on SLURM file(s)
	 */
	private static WatchService watchService;

	/**
	 * Class logger
	 */
	private static final Logger logger = Logger.getLogger(QuartzSlurmWatcherJob.class.getName());

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		if (watchService == null) {
			throw new JobExecutionException("The WatchService isn't configured");
		}
		Path slurmPath = SlurmManager.getSlurmLocationFile().toPath().normalize();
		String slurmFile = slurmPath.getFileName().toString();
		WatchKey key;
		try {
			while ((key = watchService.take()) != null) {
				for (WatchEvent<?> event : key.pollEvents()) {
					logger.log(Level.INFO, "File " + event.context() + " update detected, kind: " + event.kind());
					if (!event.context().toString().equals(slurmFile)) {
						// Not interested on another file(s)
						continue;
					}
					if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
						continue;
					}
					if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
						// Delete all
						try {
							DataAccessService.getSlurmDAO().deleteSlurm();
						} catch (ApiDataAccessException e) {
							logger.log(Level.SEVERE, "Error deleting SLURM from the Data Access Implementation", e);
						}
						continue;
					}
					logger.log(Level.INFO, "Triggering SLURM validation");
					MasterScheduler.triggerSlurmValidation();
				}
				boolean valid = key.reset();
				if (!valid) {
					break;
				}
			}
		} catch (InterruptedException e) {
			logger.log(Level.INFO, "Stopping watch service", e);
		}
	}

	static JobDetail buildJob() throws SchedulerException {
		// Place the watcher at the parent directory
		Path slurmPath = SlurmManager.getSlurmLocationFile().toPath().normalize();
		Path slurmDir = slurmPath.getParent();
		try {
			watchService = FileSystems.getDefault().newWatchService();
			slurmDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			throw new SchedulerException("Error creating the watch service for " + slurmPath.toString(), e);
		}
		return JobBuilder.newJob(QuartzSlurmWatcherJob.class).withIdentity(getJobKey()).build();
	}

	static JobKey getJobKey() {
		return new JobKey(String.format("%s", QuartzSlurmWatcherJob.class.getName()));
	}
}

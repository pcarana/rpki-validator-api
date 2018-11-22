package mx.nic.lab.rpki.api.validation;

import java.util.ArrayList;
import java.util.List;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import mx.nic.lab.rpki.api.slurm.SlurmManager;

/**
 * {@link Job} used to load the SLURM from a file. This Job is expected to be
 * executed only on demand.
 *
 */
@DisallowConcurrentExecution
public class QuartzSlurmLoaderJob implements Job {

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		List<Exception> exceptions = new ArrayList<>();
		SlurmManager.loadSlurmFromFile(exceptions);
		if (!exceptions.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (Exception e : exceptions) {
				sb.append("[").append(e.getMessage()).append(", ").append(e.getCause()).append("] ");
			}
			throw new JobExecutionException("Error loading or using the SLURM at " + SlurmManager.getSlurmLocationFile()
					+ ". " + sb.toString());
		}
	}

	static JobDetail buildJob() {
		return JobBuilder.newJob(QuartzSlurmLoaderJob.class).storeDurably().withIdentity(getJobKey()).build();
	}

	static JobKey getJobKey() {
		return new JobKey(String.format("%s", QuartzSlurmLoaderJob.class.getName()));
	}
}

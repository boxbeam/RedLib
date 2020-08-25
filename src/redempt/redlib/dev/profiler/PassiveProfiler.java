package redempt.redlib.dev.profiler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A profiler best used over long periods of time. Uses very little memory even when running
 * for very long periods, but cannot retrieve data from specific timeframes - only allows
 * summaries of the entire time that was profiled.
 * @author Redempt
 */
public class PassiveProfiler extends Profiler {
	
	private Thread server;
	private ScheduledExecutorService scheduler;
	private SampleSummary summary;
	
	/**
	 * Create a new PassiveProfiler with an empty summary
	 */
	public PassiveProfiler() {
		summary = new SampleSummary();
	}
	
	/**
	 * Start this profiler. Must be run from the thread you intend to profile.
	 */
	public void start() {
		summary = new SampleSummary();
		if (server != null) {
			return;
		}
		server = Thread.currentThread();
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(() -> {
			new Thread(() -> {
				summary.add(server.getStackTrace());
			}).start();
		}, 1, 1, TimeUnit.MILLISECONDS);
	}
	
	protected void end() {
		if (scheduler != null) {
			scheduler.shutdown();
			scheduler = null;
		}
		server = null;
	}
	
	/**
	 * @return A summary of all of the data collected by this profiler, up to the time this method was called.
	 * The returned summary will not be updated with new data after it is returned.
	 */
	public SampleSummary getSummary() {
		return summary.clone();
	}
	
	/**
	 * @return A summary of all the data collected by this profiler. The returned summary will be updated
	 * with new data after it is returned if the profiler is still running.
	 */
	public SampleSummary getRunningSummary() {
		return summary;
	}

}

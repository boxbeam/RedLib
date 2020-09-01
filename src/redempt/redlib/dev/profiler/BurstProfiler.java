package redempt.redlib.dev.profiler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A profiler best used in bursts. Uses a lot of memory to profile even short periods of time,
 * but allows you to select certain timeframes to inspect
 * @author Redempt
 */
public class BurstProfiler extends Profiler {
	
	private Thread server;
	private ArrayBlockingQueue<Sample> samples;
	private ScheduledExecutorService scheduler;
	private int size;
	
	/**
	 * Create a new BurstProfiler with an initial size, being the number of milliseconds
	 * it will be able to record
	 * @param size
	 */
	public BurstProfiler(int size) {
		this.size = size;
		samples = new ArrayBlockingQueue<>(size);
	}
	
	/**
	 * Create a new BurstProfiler with a default size of 10,000 (10 seconds)
	 */
	public BurstProfiler() {
		this(10000);
	}
	
	/**
	 * Start this profiler. Must be run from the thread you intend to profile.
	 */
	@Override
	public void start() {
		samples.clear();
		if (server != null) {
			return;
		}
		server = Thread.currentThread();
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(() -> {
			new Thread(() -> {
				if (server == null) {
					return;
				}
				Sample sample = new Sample(server.getStackTrace(), System.currentTimeMillis());
				while (!samples.offer(sample)) {
					samples.poll();
				}
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
	 * Gets a summary of the last X milliseconds of profiling,
	 * with X being the size this BurstProfiler was initialized with
	 * @return The summary
	 */
	public SampleSummary getSummary() {
		return new SampleSummary(samples);
	}
	
	/**
	 * Gets a summary of the profiling after the specified time. Cannot go back further
	 * than X milliseconds, with X being the size this BrustProfiler was initialized with
	 * @param after The timestamp after which summary data should be included
	 * @return The summary
	 */
	public SampleSummary getSummary(long after) {
		long start = samples.element().getTime();
		long diff = after - start;
		List<Sample> list = new ArrayList<>();
		samples.stream().skip(Math.max(diff, 0)).forEach(list::add);
		return new SampleSummary(list);
	}

}

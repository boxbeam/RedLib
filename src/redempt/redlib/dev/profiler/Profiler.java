package redempt.redlib.dev.profiler;

import java.util.HashSet;
import java.util.Set;

/**
 * A base class of a Profiler which can be used to analyze the performance of the server. Runs asynchronously
 * in its own thread.
 * @author Redempt
 */
public abstract class Profiler {
	
	private static Set<Profiler> profilers = new HashSet<>();
	
	/**
	 * Stop all running profilers
	 */
	public static void stopAll() {
		profilers.forEach(Profiler::end);
		profilers.clear();
	}
	
	public Profiler() {
		profilers.add(this);
	}
	
	/**
	 * Start this profiler. Must be run from the thread you intend to profile.
	 */
	public abstract void start();
	protected abstract void end();
	
	/**
	 * @return A SampleSummary representing all of the data collected by this profiler
	 */
	public abstract SampleSummary getSummary();
	
	/**
	 * Stop this profiler
	 */
	public final void stop() {
		end();
		profilers.remove(this);
	}
	
}

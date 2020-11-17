package redempt.redlib.misc;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Simple utility for Bukkit scheduler tasks, essentially just shorthand
 * @author Redempt
 */
public class Task {
	
	/**
	 * Schedules a sync delayed task to run as soon as possible
	 * @param plugin The plugin scheduling the task
	 * @param run The task to run
	 * @return The Task that has been scheduled
	 */
	public static Task syncDelayed(Plugin plugin, Runnable run) {
		return syncDelayed(plugin, run, 0);
	}
	
	/**
	 * Schedules a sync delayed task to run as soon as possible
	 * @param plugin The plugin scheduling the task
	 * @param run The task to run
	 * @return The Task that has been scheduled
	 */
	public static Task syncDelayed(Plugin plugin, Consumer<Task> run) {
		return syncDelayed(plugin, run, 0);
	}
	
	/**
	 * Schedules a sync delayed task to run after a delay
	 * @param plugin The plugin scheduling the task
	 * @param run The task to run
	 * @param delay The delay in ticks to wait before running the task
	 * @return The Task that has been scheduled
	 */
	public static Task syncDelayed(Plugin plugin, Runnable run, long delay) {
		return syncDelayed(plugin, t -> run.run(), delay);
	}
	
	/**
	 * Schedules a sync delayed task to run after a delay
	 * @param plugin The plugin scheduling the task
	 * @param run The task to run
	 * @param delay The delay in ticks to wait before running the task
	 * @return The Task that has been scheduled
	 */
	public static Task syncDelayed(Plugin plugin, Consumer<Task> run, long delay) {
		Task[] task = {null};
		task[0] = new Task(Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> run.accept(task[0]), delay), TaskType.SYNC_DELAYED, plugin);
		return task[0];
	}
	
	/**
	 * Schedules a sync repeating task to run later
	 * @param plugin The plugin scheduling the task
	 * @param run The task to run
	 * @param delay The delay in ticks to wait before running the task
	 * @param period The number of ticks between executions of the task
	 * @return The Task that has been scheduled
	 */
	public static Task syncRepeating(Plugin plugin, Runnable run, long delay, long period) {
		return syncRepeating(plugin, t -> run.run(), delay, period);
	}
	
	/**
	 * Schedules a sync repeating task to run later
	 * @param plugin The plugin scheduling the task
	 * @param run The task to run
	 * @param delay The delay in ticks to wait before running the task
	 * @param period The number of ticks between executions of the task
	 * @return The Task that has been scheduled
	 */
	public static Task syncRepeating(Plugin plugin, Consumer<Task> run, long delay, long period) {
		Task[] task = {null};
		task[0] = new Task(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> run.accept(task[0]), delay, period), TaskType.SYNC_REPEATING, plugin);
		return task[0];
	}
	
	/**
	 * Schedules an async delayed task to run as soon as possible
	 * @param plugin The plugin scheduling the task
	 * @param run The task to run
	 * @return The Task that has been scheduled
	 */
	public static Task asyncDelayed(Plugin plugin, Runnable run) {
		return asyncDelayed(plugin, t -> run.run(), 0);
	}
	
	/**
	 * Schedules an async delayed task to run as soon as possible
	 * @param plugin The plugin scheduling the task
	 * @param run The task to run
	 * @return The Task that has been scheduled
	 */
	public static Task asyncDelayed(Plugin plugin, Consumer<Task> run) {
		return asyncDelayed(plugin, run, 0);
	}
	
	/**
	 * Schedules an async delayed task to run after a delay
	 * @param plugin The plugin scheduling the task
	 * @param run The task to run
	 * @param delay The delay in ticks to wait before running the task
	 * @return The Task that has been scheduled
	 */
	public static Task asyncDelayed(Plugin plugin, Runnable run, long delay) {
		return asyncDelayed(plugin, t -> run.run(), delay);
	}
	
	/**
	 * Schedules an async delayed task to run after a delay
	 * @param plugin The plugin scheduling the task
	 * @param run The task to run
	 * @param delay The delay in ticks to wait before running the task
	 * @return The Task that has been scheduled
	 */
	public static Task asyncDelayed(Plugin plugin, Consumer<Task> run, long delay) {
		Task[] task = {null};
		task[0] = new Task(Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> run.accept(task[0]), delay), TaskType.ASYNC_DELAYED, plugin);
		return task[0];
	}
	
	/**
	 * Schedules an async repeating task to run later
	 * @param plugin The plugin scheduling the task
	 * @param run The task to run
	 * @param delay The delay in ticks to wait before running the task
	 * @param period The number of ticks between executions of the task
	 * @return The Task that has been scheduled
	 */
	public static Task asyncRepeating(Plugin plugin, Consumer<Task> run, long delay, long period) {
		Task[] task = {null};
		task[0] = new Task(Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, () -> run.accept(task[0]), delay, period), TaskType.ASYNC_REPEATING, plugin);
		return task[0];
	}
	
	/**
	 * Schedules an async repeating task to run later
	 * @param plugin The plugin scheduling the task
	 * @param run The task to run
	 * @param delay The delay in ticks to wait before running the task
	 * @param period The number of ticks between executions of the task
	 * @return The Task that has been scheduled
	 */
	public static Task asyncRepeating(Plugin plugin, Runnable run, long delay, long period) {
		return asyncRepeating(plugin, t -> run.run(), delay, period);
	}
	
	private int task;
	private TaskType type;
	private Plugin plugin;
	
	private Task(int task, TaskType type, Plugin plugin) {
		this.task = task;
		this.type = type;
		this.plugin = plugin;
	}
	
	/**
	 * @return The type of this Task
	 */
	public TaskType getType() {
		return type;
	}
	
	/**
	 * @return Whether this Task is queued, same as {@link org.bukkit.scheduler.BukkitScheduler#isQueued(int)}
	 */
	public boolean isQueued() {
		return Bukkit.getScheduler().isQueued(task);
	}
	
	/**
	 * @return @return Whether this Task is currently running, same as {@link org.bukkit.scheduler.BukkitScheduler#isCurrentlyRunning(int)}
	 */
	public boolean isCurrentlyRunning() {
		return Bukkit.getScheduler().isCurrentlyRunning(task);
	}
	
	/**
	 * Cancels this task, same as {@link org.bukkit.scheduler.BukkitScheduler#cancelTask(int)}
	 */
	public void cancel() {
		Bukkit.getScheduler().cancelTask(task);
	}
	
	/**
	 * @return The Plugin which scheduled this task
	 */
	public Plugin getPlugin() {
		return plugin;
	}
	
	/**
	 * Represents a type of task
	 */
	public enum TaskType {
		
		SYNC_DELAYED,
		ASYNC_DELAYED,
		SYNC_REPEATING,
		ASYNC_REPEATING;
		
	}
	
}

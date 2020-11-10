package redempt.redlib;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.commandmanager.Messages;
import redempt.redlib.configmanager.ConfigHook;
import redempt.redlib.configmanager.ConfigManager;
import redempt.redlib.dev.StructureTool;
import redempt.redlib.dev.profiler.Profiler;
import redempt.redlib.dev.profiler.ProfilerCommands;
import redempt.redlib.dev.profiler.TickMonitorProfiler;
import redempt.redlib.enchants.events.PlayerChangedArmorEvent;
import redempt.redlib.protection.ProtectionPolicy;
import redempt.redlib.region.RegionEnterExitListener;

/**
 * @author Redempt
 */
public class RedLib extends JavaPlugin {
	
	@ConfigHook("devMode")
	public static boolean devMode = false;
	@ConfigHook("autoStartPassiveProfiler")
	private static boolean autoStartPassiveProfiler = false;
	@ConfigHook("autoStartTickMonitorProfiler")
	private static boolean autoStartTickMonitorProfiler = false;
	@ConfigHook("tickMonitorProfilerMinTickLength")
	private static int tickMonitorProfilerMinTickLength = 100;
	public static int midVersion = Integer.parseInt(getServerVersion().split("\\.")[1]);
	
	public static RedLib getInstance() {
		return RedLib.getPlugin(RedLib.class);
	}
	
	@Override
	public void onEnable() {
		Messages.load(this);
		new ConfigManager(this).register(this).saveDefaults().load();
		if (devMode) {
			new CommandParser(this.getResource("command.txt"))
					.setArgTypes(ArgType.of("material", Material.class))
					.parse()
					.register("redlib",
					new ProfilerCommands(),
					StructureTool.enable());
			if (autoStartPassiveProfiler) {
				ProfilerCommands.getProfiler().start();
			}
			if (autoStartTickMonitorProfiler) {
				TickMonitorProfiler.setTickMinimum(tickMonitorProfilerMinTickLength);
				TickMonitorProfiler.start();
			}
		}
		PlayerChangedArmorEvent.register();
		RegionEnterExitListener.register();
		ProtectionPolicy.registerProtections();
	}
	
	@Override
	public void onDisable() {
		Profiler.stopAll();
	}
	
	public static String getServerVersion() {
		String version = Bukkit.getVersion();
		String[] split = version.split(" ");
		return split[split.length - 1].trim().replace(")", "");
	}
	
}
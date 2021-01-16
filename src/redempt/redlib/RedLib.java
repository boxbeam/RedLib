package redempt.redlib;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.commandmanager.Messages;
import redempt.redlib.configmanager.ConfigManager;
import redempt.redlib.configmanager.annotations.ConfigValue;
import redempt.redlib.dev.ChainCommand;
import redempt.redlib.dev.StructureTool;
import redempt.redlib.dev.profiler.Profiler;
import redempt.redlib.dev.profiler.ProfilerCommands;
import redempt.redlib.dev.profiler.TickMonitorProfiler;
import redempt.redlib.enchants.events.PlayerChangedArmorEvent;
import redempt.redlib.enchants.events.PlayerChangedHeldItemEvent;
import redempt.redlib.protection.ProtectionPolicy;
import redempt.redlib.region.RegionEnterExitListener;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Redempt
 */
public class RedLib extends JavaPlugin {
	
	@ConfigValue("devMode")
	public static boolean devMode = false;
	@ConfigValue("autoStartPassiveProfiler")
	private static boolean autoStartPassiveProfiler = false;
	@ConfigValue("autoStartTickMonitorProfiler")
	private static boolean autoStartTickMonitorProfiler = false;
	@ConfigValue("tickMonitorProfilerMinTickLength")
	private static int tickMonitorProfilerMinTickLength = 100;
	/**
	 * The middle number of the server version - for example, if the server version is 1.15.2, this will be 15
	 */
	public static final int MID_VERSION = Integer.parseInt(getServerVersion().split("\\.")[1]);
	
	public static RedLib getInstance() {
		return RedLib.getPlugin(RedLib.class);
	}
	
	@Override
	public void onEnable() {
		Messages.load(this);
		new ConfigManager(this).register(this).saveDefaults().load();
		if (devMode) {
			ChainCommand chain = new ChainCommand();
			new CommandParser(this.getResource("command.txt"))
					.setArgTypes(ArgType.of("material", Material.class), chain.getArgType())
					.parse()
					.register("redlib",
					new ProfilerCommands(),
					StructureTool.enable(), chain);
			if (autoStartPassiveProfiler) {
				ProfilerCommands.getProfiler().start();
			}
			if (autoStartTickMonitorProfiler) {
				TickMonitorProfiler.setTickMinimum(tickMonitorProfilerMinTickLength);
				TickMonitorProfiler.start();
			}
		}
		PlayerChangedArmorEvent.register();
		PlayerChangedHeldItemEvent.register();
		RegionEnterExitListener.register();
		ProtectionPolicy.registerProtections();
	}
	
	@Override
	public void onDisable() {
		Profiler.stopAll();
	}
	
	/**
	 * @return The server version String (ex: 1.16.4)
	 */
	public static String getServerVersion() {
		String version = Bukkit.getVersion();
		String[] split = version.split(" ");
		return split[split.length - 1].trim().replace(")", "");
	}
	
	/**
	 * Gets the plugin that called the calling method of this method
	 * @return The plugin which called the method
	 */
	public static Plugin getCallingPlugin() {
		Exception ex = new Exception();
		try {
			Class<?> clazz = Class.forName(ex.getStackTrace()[2].getClassName());
			Plugin plugin = JavaPlugin.getProvidingPlugin(clazz);
			return plugin.isEnabled() ? plugin : Bukkit.getPluginManager().getPlugin(plugin.getName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets all non-abstract, non-interface classes which extend a certain class within a plugin
	 * @param plugin The plugin
	 * @param clazz The class
	 * @param <T> The type of the class
	 * @return The list of matching classes
	 */
	public static <T> List<Class<? extends T>> getExtendingClasses(Plugin plugin, Class<T> clazz) {
		List<Class<? extends T>> list = new ArrayList<>();
		try {
			ClassLoader loader = plugin.getClass().getClassLoader();
			JarFile file = new JarFile(new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()));
			Enumeration<JarEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				String name = entry.getName();
				if (!name.endsWith(".class")) {
					continue;
				}
				name = name.substring(0, name.length() - 6).replace("/", ".");
				Class<?> c;
				try {
					c = Class.forName(name, true, loader);
				} catch (ClassNotFoundException ex) {
					continue;
				}
				if (!clazz.isAssignableFrom(c) || Modifier.isAbstract(c.getModifiers()) || c.isInterface()) {
					continue;
				}
				list.add((Class<? extends T>) c);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return list;
	}
	
}
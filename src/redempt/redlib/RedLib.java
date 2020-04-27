package redempt.redlib;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.commandmanager.Messages;
import redempt.redlib.dev.ItemHelper;
import redempt.redlib.dev.StructureTool;

public class RedLib extends JavaPlugin {
	
	public static boolean devMode = false;
	public static Plugin plugin;
	public static int midVersion = Integer.parseInt(getServerVersion().split("\\.")[1]);
	
	private static Messages messages;
	
	public static String getMessage(String message) {
		return messages.get(message);
	}
	
	@Override
	public void onEnable() {
		plugin = this;
		messages = Messages.load(this);
		FileConfiguration config = this.getConfig();
		if (config.contains("devMode")) {
			devMode = config.getBoolean("devMode");
		} else {
			config.set("devMode", false);
			this.saveConfig();
		}
		
		if (devMode) {
			new CommandParser(this.getResource("command.txt"))
					.parse()
					.register("redlib",
					new ItemHelper(),
					StructureTool.enable());
		}
	}
	
	public static String getServerVersion() {
		String version = Bukkit.getVersion();
		String[] split = version.split(" ");
		return split[split.length - 1].trim().replace(")", "");
	}
	
}
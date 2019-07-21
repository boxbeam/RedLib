package redempt.redlib;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class RedLib extends JavaPlugin {
	
	public static String helpTitle;
	public static String helpEntry;
	public static Plugin plugin;
	
	@Override
	public void onEnable() {
		plugin = this;
		FileConfiguration config = this.getConfig();
		if (config.getString("helpTitle") == null) {
			config.set("helpTitle", "&a--[ &eHelp for %cmdname% &a]--");
			this.saveConfig();
		}
		if (config.getString("helpEntry") == null) {
			config.set("helpEntry", "&e%cmdname%&a: %help%");
			this.saveConfig();
		}
		helpTitle = config.getString("helpTitle");
		helpEntry = config.getString("helpEntry");
	}
	
	public static String getServerVersion() {
		String version = Bukkit.getVersion();
		String[] split = version.split(" ");
		return split[split.length - 1].trim().replace(")", "");
	}
	
}
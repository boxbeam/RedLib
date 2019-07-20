package redempt.redlib;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class RedLib extends JavaPlugin implements Listener {
	
	public static String helpTitle;
	public static String helpEntry;
	public static Plugin plugin;
	
	@Override
	public void onEnable() {
		plugin = this;
		getServer().getPluginManager().registerEvents(this, this);
		FileConfiguration config = this.getConfig();
		if (config.getString("helpTitle") == null) {
			config.set("helpTitle", "&a--[ &eHelp for %cmdname% &a]--");
		}
		if (config.getString("helpEntry") == null) {
			config.set("helpEntry", "&e%cmdname%&a: %help%");
		}
		helpTitle = config.getString("helpTitle");
		helpEntry = config.getString("helpEntry");
	}
	
}
package redempt.cmdmgr2;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CmdMgr extends JavaPlugin implements Listener {
	
	public static String helpTitle;
	public static String helpEntry;
	public static Plugin plugin;
	
	@Override
	public void onEnable() {
		plugin = this;
		getServer().getPluginManager().registerEvents(this, this);
		ConfigHandler handler = new ConfigHandler("config.txt", this);
		handler.addDefault("helpTitle", "&a--[ &eHelp for %cmdname% &a]--");
		handler.addDefault("helpEntry", "&e%cmdname%&a: %help%");
		handler.loadConfig();
		helpTitle = handler.getProperty("helpTitle");
		helpEntry = handler.getProperty("helpEntry");
	}
	
}
package redempt.redlib;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import redempt.redlib.commandmanager.Command;
import redempt.redlib.commandmanager.CommandBuilder;
import redempt.redlib.dev.ItemHelper;
import redempt.redlib.dev.StructureTool;

public class RedLib extends JavaPlugin {
	
	public static String helpTitle;
	public static String helpEntry;
	public static boolean devMode = false;
	public static Plugin plugin;
	public static int midVersion = Integer.parseInt(getServerVersion().split("\\.")[1]);
	
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
		if (config.contains("devMode")) {
			devMode = config.getBoolean("devMode");
		}
		helpTitle = config.getString("helpTitle");
		helpEntry = config.getString("helpEntry");
		
		if (devMode) {
			Command.fromStream(this.getResource("command.txt"))
					.register("redlib",
					new ItemHelper(),
					StructureTool.enable());
		} else {
			new CommandBuilder("struct", "structure", "itemhelper", "itemhelp", "ih")
				.permission("redlib.admin")
				.help("Developer mode is disabled.")
				.hook((c) -> {
					c.sendMessage(ChatColor.RED + "RedLib Developer Mode is disabled for this server.");
					c.sendMessage(ChatColor.RED + "To enable Developer Mode, add 'devMode: true' to the RedLib config and restart the server.");
				})
				.build("redlib");
		}
	}
	
	public static String getServerVersion() {
		String version = Bukkit.getVersion();
		String[] split = version.split(" ");
		return split[split.length - 1].trim().replace(")", "");
	}
	
}
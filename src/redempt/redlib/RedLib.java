package redempt.redlib;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import redempt.redlib.commandmanager.CommandBuilder;
import redempt.redlib.commandmanager.CommandFactory;
import redempt.redlib.dev.ItemHelper;
import redempt.redlib.dev.StructureTool;

public class RedLib extends JavaPlugin {
	
	public static boolean devMode = false;
	public static Plugin plugin;
	public static int midVersion = Integer.parseInt(getServerVersion().split("\\.")[1]);
	
	private void setDefault(FileConfiguration config, String key, String value) {
		config.set(key, config.get(key, value));
	}
	
	private void setConfigDefaults() {
		FileConfiguration config = this.getConfig();
		setDefault(config, "helpTitle", "&a--[ &eHelp for %cmdname% &a]--");
		setDefault(config, "helpEntry", "&e%cmdname%&a: %help%");
		setDefault(config, "noPermission", "&cYou do not have permission to run this command! (%permission%)");
		setDefault(config, "firstLocationSet", "&aFirst location set!");
		setDefault(config, "secondLocationSet", "&aSecond location set!");
		setDefault(config, "cancelPromptMessage", "&cType '%canceltext%' to cancel.");
		setDefault(config, "cancelText", "cancel");
		setDefault(config, "mustHoldItem", "&cYou must be holding an item to do this!");
		this.saveConfig();
	}
	
	public static String getMessage(String message) {
		return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(message));
	}
	
	@Override
	public void onEnable() {
		plugin = this;
		setConfigDefaults();
		FileConfiguration config = this.getConfig();
		if (config.contains("devMode")) {
			devMode = config.getBoolean("devMode");
		}
		
		if (devMode) {
			new CommandFactory(this.getResource("command.txt"))
					.parse()
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
package redempt.redlib.commandmanager;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import redempt.redlib.RedLib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a list of messages loaded from a file with defaults
 * @author Redempt
 *
 */
public class Messages {
	
	private static Map<Plugin, Messages> all = new HashMap<>();
	
	/**
	 * Loads messages from a file and writes missing defaults
	 * @param plugin The plugin loading the messages
	 * @param defaults The InputStream for default messages. Use {@link Plugin#getResource(String)} for this.
	 * @param filename The name of the file in the plugin folder to load messages from
	 * @return The Messages instance with messages loaded.
	 */
	public static Messages load(Plugin plugin, InputStream defaults, String filename) {
		java.nio.file.Path path = plugin.getDataFolder().toPath().resolve(filename);
		try {
			Map<String, String> messages = Files.exists(path) ? parse(Files.readAllLines(path)) : new LinkedHashMap<>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(defaults));
			List<String> lines = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			Map<String, String> defaultMap = parse(lines);
			boolean[] missing = {false};
			defaultMap.forEach((k, v) -> {
				if (!messages.containsKey(k)) {
					messages.put(k, v);
					missing[0] = true;
				}
			});
			if (missing[0]) {
				write(messages, path);
			}
			return new Messages(plugin, messages, defaultMap);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Loads messages from a file, messages.txt, and writes missing defaults
	 * @param plugin The plugin loading the messages
	 * @param defaults The InputStream for default messages. Use {@link Plugin#getResource(String)} for this.
	 * @return The Messages instance with messages loaded.
	 */
	public static Messages load(Plugin plugin, InputStream defaults) {
		return load(plugin, defaults, "messages.txt");
	}
	
	/**
	 * Loads messages from a file, messages.txt, and writes missing defaults loaded from the plugin resource called messages.txt
	 * @param plugin The plugin loading the messages
	 * @return The Messages instance with messages loaded.
	 */
	public static Messages load(Plugin plugin) {
		return load(plugin, plugin.getResource("messages.txt"), "messages.txt");
	}
	
	/**
	 * Determines which plugin is calling this method, finds its loaded messages, and returns the message with the given name.
	 * @param message The name of the message
	 * @return The message, which has been formatted with {@literal &} as the color character.
	 * @throws IllegalStateException if your plugin has not loaded any messages
	 */
	public static String msg(String message) {
		Plugin plugin = RedLib.getCallingPlugin();
		Messages msgs = all.get(plugin);
		if (msgs == null) {
			throw new IllegalStateException("Your plugin has not loaded any messages, or this method is being called from the wrong plugin");
		}
		return msgs.get(message);
	}
	
	private static Map<String, String> parse(List<String> input) {
		Map<String, String> map = new LinkedHashMap<>();
		for (String s : input) {
			if (s == null) {
				break;
			}
			int index = s.indexOf(':');
			map.put(s.substring(0, index), s.substring(index + 1).trim());
		}
		return map;
	}
	
	private static void write(Map<String, String> map, java.nio.file.Path file) {
		List<String> lines = map.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.toList());
		try {
			if (!Files.exists(file.getParent())) {
				Files.createDirectory(file.getParent());
			}
			Files.write(file, lines, Charset.defaultCharset(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Plugin plugin;
	private Map<String, String> messages;
	private Map<String, String> defaults;
	
	private Messages(Plugin plugin, Map<String, String> messages, Map<String, String> defaults) {
		this.messages = messages;
		this.defaults = defaults;
		this.plugin = plugin;
		all.put(plugin, this);
	}
	
	/**
	 * @return The plugin these messages belong to
	 */
	public Plugin getPlugin() {
		return plugin;
	}
	
	/**
	 * Gets a color-formatted message by name
	 * @param msg The name of the message
	 * @return The message, which has been formatted with {@literal &} as the color character.
	 */
	public String get(String msg) {
		String message = messages.getOrDefault(msg, defaults.get(msg));
		if (message == null) {
			throw new IllegalArgumentException("Message '" + msg + "' does not have an assigned or default value!");
		}
		return ChatColor.translateAlternateColorCodes('&', message);
	}
	
}

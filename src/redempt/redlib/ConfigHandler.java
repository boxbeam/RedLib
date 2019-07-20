package redempt.cmdmgr2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.plugin.Plugin;

public class ConfigHandler {
	
	private List<String> defaults = new ArrayList<>();
	private Map<String, String> properties = new HashMap<>();
	private String filename;
	private Plugin owner;
	
	public ConfigHandler(String filename, Plugin plugin) {
		this.filename = filename;
		this.owner = plugin;
	}
	
	public void addDefault(String name, String value) {
		defaults.add(name + ":" + value);
	}
	
	public void loadConfig() {
		Path path = Paths.get(owner.getDataFolder().toURI()).resolve(filename);
		try {
			if (!Files.exists(path)) {
				Files.createDirectories(path.getParent());
				Files.write(path, defaults, StandardOpenOption.CREATE);
			}
			Files.lines(path).forEach((s) -> {
				String[] split = s.split(":");
				if (split.length < 2) {
					return;
				}
				if (split[1].equals("<none>")) {
					properties.put(split[0], null);
					return;
				}
				String[] copy = new String[split.length - 1];
				System.arraycopy(split, 1, copy, 0, split.length - 1);
				properties.put(split[0], String.join(":", copy));
			});
		} catch (IOException e) {
			
		}
	}
	
	public String getProperty(String name) {
		return properties.get(name);
	}
	
}

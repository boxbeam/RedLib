package redempt.redlib.configmanager;

import org.bukkit.configuration.ConfigurationSection;

public interface ConfigStorage {
	
	void init(ConfigManager manager);
	void save(ConfigurationSection section);
	void load(ConfigurationSection section);
	
}

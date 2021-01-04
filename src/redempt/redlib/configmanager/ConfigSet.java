package redempt.redlib.configmanager;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;

class ConfigSet<T> extends HashSet<T> implements ConfigStorage {
	
	protected Class<T> clazz;
	private ConfigObjectMapper<T> mapper;
	private ConfigurationSection section;
	private ConfigManager manager;
	private ConversionType type;
	
	public ConfigSet(Class<T> clazz, ConversionType type) {
		this.clazz = clazz;
		this.type = type;
	}
	
	@Override
	public void init(ConfigManager manager) {
		if (this.manager != null) {
			return;
		}
		mapper = new ConfigObjectMapper<>(clazz, type, manager);
		this.manager = manager;
		
	}
	
	@Override
	public void save(ConfigurationSection section) {
		int[] count = {0};
		forEach(i -> {
			mapper.save(section, count[0] + "", i);
			count[0]++;
		});
	}
	
	@Override
	public void load(ConfigurationSection section) {
		clear();
		section.getKeys(false).forEach(k -> {
			add(mapper.load(section, k));
		});
	}
	
}

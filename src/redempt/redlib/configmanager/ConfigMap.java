package redempt.redlib.configmanager;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.configmanager.annotations.ConfigMappable;
import redempt.redlib.configmanager.exceptions.ConfigMapException;

import java.util.HashMap;

class ConfigMap<T> extends HashMap<String, T> implements ConfigStorage {
	
	protected Class<T> clazz;
	protected ConfigurationSection section;
	private ConfigManager manager;
	private ConfigObjectMapper<T> mapper;
	
	public ConfigMap(Class<T> clazz) {
		this.clazz = clazz;
	}
	
	public void init(ConfigManager manager) {
		if (this.manager != null) {
			return;
		}
		if (clazz.isAnnotationPresent(ConfigMappable.class)) {
			mapper = new ConfigObjectMapper<>(clazz, manager);
		}
		this.manager = manager;
	}
	
	public void load(ConfigurationSection section) {
		this.section = section;
		if (clazz.isAnnotationPresent(ConfigMappable.class)) {
			section.getKeys(false).forEach(k -> {
				put(k, mapper.load(section.getConfigurationSection(k)));
			});
			return;
		}
		TypeConverter<T> converter = (TypeConverter<T>) manager.converters.get(clazz);
		if (converter == null) {
			throw new ConfigMapException("No converter for class " + clazz.getName());
		}
		section.getKeys(false).forEach(k -> {
			put(k, converter.load(section.getString(k)));
		});
	}
	
	public void save(ConfigurationSection section) {
		this.section = section;
		if (clazz.isAnnotationPresent(ConfigMappable.class)) {
			forEach((k, v) -> {
				ConfigurationSection sect = section.createSection(k);
				mapper.save(sect, v);
			});
			return;
		}
		TypeConverter<T> converter = (TypeConverter<T>) manager.converters.get(clazz);
		if (converter == null) {
			throw new ConfigMapException("No converter for class " + clazz.getName());
		}
		forEach((k, v) -> {
			section.set(k, converter.save(v));
		});
	}
	
	@Override
	public T put(String key, T value) {
		T out = super.put(key, value);
		if (section != null && mapper != null) {
			ConfigurationSection section = this.section.getConfigurationSection(key);
			section = section == null ? this.section.createSection(key) : section;
			mapper.setPathField(value, section);
		}
		return out;
	}
	
}

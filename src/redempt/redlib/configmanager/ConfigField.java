package redempt.redlib.configmanager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.configmanager.exceptions.ConfigFieldException;
import redempt.redlib.configmanager.exceptions.ConfigListException;
import redempt.redlib.configmanager.exceptions.ConfigMapException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ConfigField {
	
	private Field field;
	private String path;
	private ConfigManager manager;
	protected TypeConverter<?> converter;
	protected int priority;
	
	public ConfigField(Field field, String path, int priority, ConfigManager manager) {
		this.field = field;
		this.path = path;
		this.priority = priority;
		this.manager = manager;
		converter = manager.converters.get(field.getType());
		field.setAccessible(true);
		if (Modifier.isFinal(field.getModifiers())) {
			throw new ConfigFieldException("Config hook field may not be final!");
		}
	}
	
	public <T> void load(Object object, ConfigurationSection config) {
		try {
			if (field.get(object) instanceof ConfigStorage) {
				ConfigStorage storage = (ConfigStorage) field.get(object);
				storage.init(manager);
				ConfigurationSection section = config.getConfigurationSection(path);
				if (section == null) {
					return;
				}
				storage.load(section);
				field.set(object, storage);
				return;
			}
			if (Map.class.isAssignableFrom(field.getType())) {
				Map map = (Map) field.get(object);
				ConfigurationSection section = config.getConfigurationSection(path);
				if (section == null) {
					return;
				}
				section.getKeys(false).forEach(k -> {
					map.put(k, section.get(k));
				});
				return;
			}
			Object value;
			if (converter != null) {
				value = converter.load(config.getString(path));
			} else {
				value = config.get(path);
			}
			if (value != null) {
				field.set(object, value);
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public void save(Object object, ConfigurationSection config) {
		try {
			if (field.get(object) instanceof ConfigStorage) {
				ConfigStorage storage = (ConfigStorage) field.get(object);
				storage.init(manager);
				ConfigurationSection section = config.createSection(path);
				storage.save(section);
				return;
			}
			Object value = field.get(object);
			if (converter != null) {
				value = converter.save(value);
			}
			config.set(path, value);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public void saveIfAbsent(Object object, ConfigurationSection config) {
		try {
			if (!config.isSet(path) && field.get(object) != null) {
				save(object, config);
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public String getPath() {
		return path;
	}
	
}

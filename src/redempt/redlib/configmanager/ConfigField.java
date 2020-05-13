package redempt.redlib.configmanager;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.configmanager.exceptions.ConfigFieldException;
import redempt.redlib.configmanager.exceptions.ConfigMapException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

class ConfigField {
	
	private Field field;
	private String path;
	
	public ConfigField(Field field, String path) {
		this.field = field;
		this.path = path;
		field.setAccessible(true);
		if (Modifier.isFinal(field.getModifiers())) {
			throw new ConfigFieldException("Config hook field may not be final!");
		}
	}
	
	public void load(Object object, ConfigurationSection config) {
		try {
			if (path.endsWith(".*")) {
				String name = path.substring(0, path.length() - 2);
				ConfigurationSection section = config.getConfigurationSection(name);
				section = section == null ? config.createSection(name) : section;
				Object obj = field.get(object);
				if (!(obj instanceof ConfigMap)) {
					throw new ConfigMapException("Paths ending with .* must be a ConfigMap created using ConfigManager.map(Class)");
				}
				ConfigMap<?> map = (ConfigMap<?>) obj;
				map.section = section;
				map.load();
				return;
			}
			Object value = config.get(path);
			if (value != null) {
				field.set(object, value);
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public void save(Object object, ConfigurationSection config) {
		try {
			if (path.endsWith(".*")) {
				ConfigMap<?> map = (ConfigMap<?>) field.get(object);
				map.save();
				return;
			}
			config.set(path, field.get(object));
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public void saveIfAbsent(Object object, ConfigurationSection config) {
		try {
			if (path.endsWith(".*")) {
				String name = path.substring(0, path.length() - 2);
				ConfigMap<?> map = (ConfigMap<?>) field.get(object);
				ConfigurationSection section = config.getConfigurationSection(name);
				if (section == null) {
					section = config.createSection(name);
					map.section = section;
					map.save();
				}
				return;
			}
			if (!config.isSet(path) && field.get(object) != null) {
				save(object, config);
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
}

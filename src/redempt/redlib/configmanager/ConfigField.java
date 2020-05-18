package redempt.redlib.configmanager;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.configmanager.exceptions.ConfigFieldException;
import redempt.redlib.configmanager.exceptions.ConfigListException;
import redempt.redlib.configmanager.exceptions.ConfigMapException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

class ConfigField {
	
	private Field field;
	private String path;
	private ConfigManager manager;
	private TypeConverter<?> converter;
	
	public ConfigField(Field field, String path, ConfigManager manager) {
		this.field = field;
		this.path = path;
		this.manager = manager;
		converter = manager.converters.get(field.getType());
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
				map.manager = manager;
				map.init();
				map.load();
				return;
			}
			if (List.class.isAssignableFrom(field.getType())) {
				Object val = field.get(object);
				if (val instanceof ConfigList) {
					ConfigList<?> list = (ConfigList<?>) val;
					Class<?> clazz = list.clazz;
					List<String> strings = config.getStringList(path);
					if (strings != null && strings.size() > 0) {
						list.clear();
						TypeConverter<?> converter = manager.converters.get(clazz);
						strings.stream().map(converter::load).forEach(list::castAdd);
						if (converter == null) {
							throw new ConfigListException("No TypeConverter exists for type " + clazz.getName());
						}
					}
					return;
				}
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
			if (path.endsWith(".*")) {
				ConfigMap<?> map = (ConfigMap<?>) field.get(object);
				map.save();
				return;
			}
			if (List.class.isAssignableFrom(field.getType())) {
				Object val = field.get(object);
				if (val instanceof ConfigList) {
					ConfigList<?> list = (ConfigList<?>) val;
					Class<?> clazz = list.clazz;
					TypeConverter<?> converter = manager.converters.get(clazz);
					if (converter == null) {
						throw new ConfigListException("No TypeConverter exists for type " + clazz.getName());
					}
					List<String> strings = new ArrayList<>();
					list.stream().map(converter::save).forEach(strings::add);
					config.set(path, strings);
					return;
				}
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

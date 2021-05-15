package redempt.redlib.configmanager;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.configmanager.exceptions.ConfigFieldException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

class ConfigField {
	
	private Field field;
	private String path;
	private ConfigManager manager;
	private ConversionType type;
	private ConfigObjectMapper<?> mapper;
	protected TypeConverter<?> converter;
	protected int priority;
	
	public ConfigField(ConversionType type, Field field, String path, int priority, ConfigManager manager) {
		if (type == ConversionType.AUTO) {
			type = ConversionType.auto(field.getType(), manager);
		}
		if (type == ConversionType.MAPPED_OBJECT) {
			this.mapper = new ConfigObjectMapper<>(field.getType(), ConversionType.MAPPED_OBJECT, manager);
		}
		this.type = type;
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
			} else if (type == ConversionType.MAPPED_OBJECT) {
				value = mapper.load(config, path);
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
			if (type == ConversionType.MAPPED_OBJECT) {
				saveMapped(mapper, config, path, value);
				return;
			}
			config.set(path, value);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	private <T> void saveMapped(ConfigObjectMapper<T> mapper, ConfigurationSection section, String path, Object value) {
		mapper.save(section, path, (T) value);
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

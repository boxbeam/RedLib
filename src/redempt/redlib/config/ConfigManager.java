package redempt.redlib.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import redempt.redlib.config.annotations.ConfigMappable;
import redempt.redlib.config.conversion.CollectionConverter;
import redempt.redlib.config.conversion.EnumConverter;
import redempt.redlib.config.conversion.MapConverter;
import redempt.redlib.config.conversion.NativeConverter;
import redempt.redlib.config.conversion.ObjectConverter;
import redempt.redlib.config.conversion.StaticRootConverter;
import redempt.redlib.config.conversion.StringConverter;
import redempt.redlib.config.conversion.TypeConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Serializes and deserializes configuration data into objects and fields
 * @author Redempt
 */
public class ConfigManager {
	
	/**
	 * Creates a ConfigManager targetting a specific config file, which will be created if it does not exist
	 * @param file The config file to manage
	 * @return The ConfigManager
	 */
	public static ConfigManager create(File file) {
		return new ConfigManager(file);
	}
	
	/**
	 * Creates a ConfigManager targetting a specific config file, which will be created if it does not exist
	 * @param path The config file to manage
	 * @return The ConfigManager
	 */
	public static ConfigManager create(Path path) {
		return create(path.toFile());
	}
	
	/**
	 * Creates a ConfigManager for a file in a plugin's data folder
	 * @param plugin The plugin whose data folder should be used
	 * @param configName The name of the config file to manage
	 * @return The ConfigManager
	 */
	public static ConfigManager create(Plugin plugin, String configName) {
		return create(plugin.getDataFolder().toPath().resolve(configName));
	}
	
	/**
	 * Creates a ConfigManager for the default config in a plugin's data folder, called config.yml
	 * @param plugin The plugin whose data folder should be used
	 * @return The ConfigManager
	 */
	public static ConfigManager create(Plugin plugin) {
		return create(plugin, "config.yml");
	}
	
	private Map<ConfigType<?>, TypeConverter<?>> convertersByType;
	
	{
		convertersByType = new HashMap<>();
		convertersByType.put(new ConfigType<>(String.class), StringConverter.create(s -> s, s -> s));
		convertersByType.put(new ConfigType<>(int.class), StringConverter.create(Integer::parseInt, String::valueOf));
		convertersByType.put(new ConfigType<>(double.class), StringConverter.create(Double::parseDouble, String::valueOf));
		convertersByType.put(new ConfigType<>(float.class), StringConverter.create(Float::parseFloat, String::valueOf));
		convertersByType.put(new ConfigType<>(boolean.class), StringConverter.create(Boolean::parseBoolean, String::valueOf));
		convertersByType.put(new ConfigType<>(long.class), StringConverter.create(Long::parseLong, String::valueOf));
	}
	
	private FileConfiguration config;
	private File file;
	private TypeConverter<?> converter;
	private Object target;
	private Class<?> targetClass;
	
	private ConfigManager(File file) {
		this.file = file;
		file.getParentFile().mkdirs();
		if (file.exists()) {
			config = YamlConfiguration.loadConfiguration(file);
		} else {
			config = new YamlConfiguration();
		}
	}
	
	/**
	 * Specifies the given Object to load config values to and save config values from. The Object
	 * must be from a ConfigMappable class, and all of its non-transient fields will be worked with.
	 * @param obj The Object to operate on
	 * @return This ConfigManager
	 */
	public ConfigManager target(Object obj) {
		if (target != null || targetClass != null) {
			throw new IllegalStateException("ConfigManager already has a target");
		}
		target = obj;
		converter = ObjectConverter.create(this, new ConfigType<>(obj.getClass()));
		return this;
	}
	
	/**
	 * Specifies the given Class to load config values to and save config values from. The Class's
	 * static non-transient fields will be worked with.
	 * @param clazz The Class to operate on
	 * @return This ConfigManager
	 */
	public ConfigManager target(Class<?> clazz) {
		if (target != null || targetClass != null) {
			throw new IllegalStateException("ConfigManager already has a target");
		}
		targetClass = clazz;
		converter = StaticRootConverter.create(this, clazz);
		return this;
	}
	
	/**
	 * Saves all of the values from the target to config
	 * @return This ConfigManager
	 */
	public ConfigManager save() {
		save(converter, true);
		return this;
	}
	
	/**
	 * Saves only values which are not already specified in config
	 * @return This ConfigManager
	 */
	public ConfigManager saveDefaults() {
		save(converter, false);
		return this;
	}
	
	/**
	 * Loads all values from the current in-memory config into the target
	 * @return This ConfigManager
	 */
	public ConfigManager load() {
		load(converter);
		return this;
	}
	
	private <T> void load(TypeConverter<T> converter) {
		converter.loadFrom(config, null, (T) target);
	}
	
	/**
	 * Loads the config from disk, then loads all values into the target
	 * @return This ConfigManager
	 */
	public ConfigManager reload() {
		config = YamlConfiguration.loadConfiguration(file);
		return load();
	}
	
	private <T> void save(TypeConverter<T> converter, boolean overwrite) {
		converter.saveTo((T) target, config, null, overwrite);
		try {
			config.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return The configuration file wrapped by this ConfigManager
	 */
	public FileConfiguration getConfig() {
		return config;
	}
	
	/**
	 * Gets the TypeConverter for a given type
	 * @param type The ConfigType
	 * @param <T> The parameter type
	 * @return The TypeConverter, making a new one if none exists for the given type
	 */
	public <T> TypeConverter<T> getConverter(ConfigType<T> type) {
		if (type == null) {
			return null;
		}
		TypeConverter<T> converter = (TypeConverter<T>) convertersByType.get(type);
		if (converter == null) {
			converter = (TypeConverter<T>) createConverter(type);
			convertersByType.put(type, converter);
		}
		return converter;
	}
	
	/**
	 * Registers a string converter to this ConfigManager
	 * @param clazz The class type the converter is for
	 * @param loader A function to convert a string to the given type
	 * @param saver A function to convert the given type to a string
	 * @param <T> The type
	 * @return This ConfigManager
	 */
	public <T> ConfigManager addConverter(Class<T> clazz, Function<String, T> loader, Function<T, String> saver) {
		convertersByType.put(new ConfigType<>(clazz), StringConverter.create(loader, saver));
		return this;
	}
	
	/**
	 * Registers a converter to this ConfigManager
	 * @param type The type the converter is for
	 * @param converter The converter
	 * @param <T> The type
	 * @return This ConfigManager
	 */
	public <T> ConfigManager addConverter(ConfigType<T> type, TypeConverter<T> converter) {
		convertersByType.put(type, converter);
		return this;
	}
	
	/**
	 * Gets a StringConverter for a given type
	 * @param type The type to get a StringConverter for
	 * @param <T> The type
	 * @return The StringConverter associated with the given type
	 * @throws IllegalStateException If a StringConverter does not exist for the given type
	 */
	public <T> StringConverter<T> getStringConverter(ConfigType<T> type) {
		TypeConverter<T> keyConverter = (TypeConverter<T>) convertersByType.get(type);
		if (!(keyConverter instanceof StringConverter)) {
			throw new IllegalStateException("No appropriate string converter for key type " + type);
		}
		return (StringConverter<T>) keyConverter;
	}
	
	private <T> TypeConverter<?> createConverter(ConfigType<?> type) {
		if (Enum.class.isAssignableFrom(type.getType())) {
			return EnumConverter.create(type.getType());
		}
		if (Collection.class.isAssignableFrom(type.getType())) {
			return CollectionConverter.create(this, type);
		}
		if (Map.class.isAssignableFrom(type.getType())) {
			return MapConverter.create(this, type);
		}
		if (type.getType().isAnnotationPresent(ConfigMappable.class) || type.getType().getSuperclass().getName().equals("java.lang.Record")) {
			return ObjectConverter.create(this, type);
		}
		return NativeConverter.create();
	}
	
}

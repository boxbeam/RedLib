package redempt.redlib.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import redempt.redlib.config.annotations.ConfigMappable;
import redempt.redlib.config.annotations.ConfigSubclassable;
import redempt.redlib.config.conversion.*;
import redempt.redlib.config.data.ConfigurationSectionDataHolder;
import redempt.redlib.config.data.DataHolder;
import redempt.redlib.config.instantiation.Instantiator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Serializes and deserializes configuration data into objects and fields
 * @author Redempt
 */
public class ConfigManager {
	
	/**
	 * Creates a ConfigManager targetting a specific config file, which will be created if it does not exist
	 * @param plugin The plugin the ConfigManager belongs to
	 * @param file The config file to manage
	 * @return The ConfigManager
	 */
	public static ConfigManager create(Plugin plugin, File file) {
		return new ConfigManager(plugin, file);
	}
	
	/**
	 * Creates a ConfigManager targetting a specific config file, which will be created if it does not exist
	 * @param plugin The plugin the ConfigManager belongs to
	 * @param path The config file to manage
	 * @return The ConfigManager
	 */
	public static ConfigManager create(Plugin plugin, Path path) {
		return create(plugin, path.toFile());
	}
	
	/**
	 * Creates a ConfigManager for a file in a plugin's data folder
	 * @param plugin The plugin whose data folder should be used
	 * @param configName The name of the config file to manage
	 * @return The ConfigManager
	 */
	public static ConfigManager create(Plugin plugin, String configName) {
		ConfigManager manager = create(plugin, plugin.getDataFolder().toPath().resolve(configName));
		manager.loader = plugin.getClass().getClassLoader();
		return manager;
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
	private Map<String, Class<?>> classesByName = new ConcurrentHashMap<>();
	
	{
		convertersByType = new HashMap<>();
		convertersByType.put(new ConfigType<>(String.class), StringConverter.create(s -> s, s -> s));
		convertersByType.put(new ConfigType<>(int.class), PrimitiveConverter.create(Integer::parseInt, String::valueOf));
		convertersByType.put(new ConfigType<>(double.class), PrimitiveConverter.create(Double::parseDouble, String::valueOf));
		convertersByType.put(new ConfigType<>(float.class), PrimitiveConverter.create(Float::parseFloat, String::valueOf));
		convertersByType.put(new ConfigType<>(boolean.class), PrimitiveConverter.create(Boolean::parseBoolean, String::valueOf));
		convertersByType.put(new ConfigType<>(long.class), PrimitiveConverter.create(Long::parseLong, String::valueOf));
		convertersByType.put(new ConfigType<>(Integer.class), PrimitiveConverter.create(Integer::parseInt, String::valueOf));
		convertersByType.put(new ConfigType<>(Double.class), PrimitiveConverter.create(Double::parseDouble, String::valueOf));
		convertersByType.put(new ConfigType<>(Float.class), PrimitiveConverter.create(Float::parseFloat, String::valueOf));
		convertersByType.put(new ConfigType<>(Boolean.class), PrimitiveConverter.create(Boolean::parseBoolean, String::valueOf));
		convertersByType.put(new ConfigType<>(Long.class), PrimitiveConverter.create(Long::parseLong, String::valueOf));
	}
	
	private FileConfiguration config;
	private DataHolder holder;
	private File file;
	private TypeConverter<?> converter;
	private Object target;
	private Class<?> targetClass;
	private ClassLoader loader;
	
	private ConfigManager(Plugin plugin, File file) {
		loader = plugin.getClass().getClassLoader();
		this.file = file;
		file.getParentFile().mkdirs();
		if (file.exists()) {
			setConfig(YamlConfiguration.loadConfiguration(file));
		} else {
			setConfig(new YamlConfiguration());
		}
	}
	
	private void setConfig(FileConfiguration config) {
		this.config = config;
		this.holder = new ConfigurationSectionDataHolder(config);
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
	 * Loads a class by name, using a cache
	 * @param name The name of the class to load
	 * @return The class
	 */
	public Class<?> loadClass(String name) {
		return classesByName.computeIfAbsent(name, k -> {
			try {
				return Class.forName(k, true, loader);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return null;
			}
		});
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
		converter.loadFrom(holder, null, (T) target);
	}
	
	/**
	 * Loads the config from disk, then loads all values into the target
	 * @return This ConfigManager
	 */
	public ConfigManager reload() {
		setConfig(YamlConfiguration.loadConfiguration(file));
		return load();
	}
	
	private <T> void save(TypeConverter<T> converter, boolean overwrite) {
		converter.saveTo((T) target, holder, null, overwrite);
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
	
	private TypeConverter<?> createConverter(ConfigType<?> type) {
		if (Enum.class.isAssignableFrom(type.getType())) {
			return EnumConverter.create(type.getType());
		}
		if (Collection.class.isAssignableFrom(type.getType())) {
			return CollectionConverter.create(this, type);
		}
		if (Map.class.isAssignableFrom(type.getType())) {
			return MapConverter.create(this, type);
		}
		if (type.getType().isAnnotationPresent(ConfigMappable.class) || Instantiator.isRecord(type.getType())) {
			Class<?> clazz = type.getType();
			boolean isAbstract = clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers());
			if (isAbstract || clazz.isAnnotationPresent(ConfigSubclassable.class)) {
				return SubclassConverter.create(this, clazz, isAbstract);
			}
			return ObjectConverter.create(this, type);
		}
		return NativeConverter.create();
	}
	
}

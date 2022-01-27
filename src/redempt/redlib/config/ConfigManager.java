package redempt.redlib.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import redempt.redlib.config.conversion.*;
import redempt.redlib.config.data.ConfigurationSectionDataHolder;
import redempt.redlib.config.data.DataHolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Serializes and deserializes configuration data into objects and fields
 * @author Redempt
 */
public class ConfigManager {
	
	private static Boolean commentsSupported;
	
	/**
	 * @return Whether comments are supported in this version
	 */
	public static boolean areCommentsSupported() {
		if (commentsSupported == null) {
			commentsSupported = Arrays.stream(ConfigurationSection.class.getMethods()).anyMatch(m -> m.getName().equals("setComments"));
		}
		return commentsSupported;
	}
	
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
		return create(plugin, plugin.getDataFolder().toPath().resolve(configName));
	}
	
	/**
	 * Creates a ConfigManager for the default config in a plugin's data folder, called config.yml
	 * @param plugin The plugin whose data folder should be used
	 * @return The ConfigManager
	 */
	public static ConfigManager create(Plugin plugin) {
		return create(plugin, "config.yml");
	}
	
	private FileConfiguration config;
	private DataHolder holder;
	private File file;
	private TypeConverter<?> converter;
	private Object target;
	private Class<?> targetClass;
	private ConversionManager conversionManager;
	
	private ConfigManager(Plugin plugin, File file) {
		conversionManager = new ConversionManager(plugin);
		this.file = file;
		file.getParentFile().mkdirs();
		if (file.exists()) {
			setConfig(YamlConfiguration.loadConfiguration(file));
		} else {
			setConfig(new YamlConfiguration());
		}
	}
	
	/**
	 * @return The ConversionManager responsible for storing and creating converters for this ConfigManager
	 */
	public ConversionManager getConversionManager() {
		return conversionManager;
	}
	
	/**
	 * Sets the ConversionManager responsible for storing and creating converters for this ConfigManager
	 * @param conversionManager The ConversionManager
	 */
	public void setConversionManager(ConversionManager conversionManager) {
		this.conversionManager = conversionManager;
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
		conversionManager.addConverter(clazz, loader, saver);
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
		conversionManager.addConverter(type, converter);
		return this;
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
		converter = ObjectConverter.create(conversionManager, new ConfigType<>(obj.getClass()));
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
		converter = StaticRootConverter.create(conversionManager, clazz);
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
		Map<String, List<String>> comments = new HashMap<>();
		converter.saveTo((T) target, holder, null, overwrite, comments);
		if (areCommentsSupported()) {
			comments.forEach(config::setComments);
		}
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
	
}

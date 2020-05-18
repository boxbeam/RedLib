package redempt.redlib.configmanager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * Loads config values into variables annotated with {@link ConfigHook}
 */
public class ConfigManager {
	
	/**
	 * Instantiates a List of Strings inline
	 * @param strings The Strings to include in the List
	 * @return The List of Strings
	 */
	public static List<String> stringList(String... strings) {
		List<String> list = new ArrayList<>();
		for (String string : strings) {
			list.add(string);
		}
		return list;
	}
	
	/**
	 * Creates a ConfigMap from a given type. A ConfigMap extends HashMap. The class is not accessible, so
	 * store it in a HashMap variable. This method must be used to set the initial value for a variable
	 * which loads in a section from config.
	 * @param clazz The class of the type that will be mapped
	 * @param <T> The type
	 * @return An empty map of the given type, which will be populated when {@link ConfigManager#load()} is called
 	 */
	public static <T> ConfigMap<T> map(Class<T> clazz) {
		return new ConfigMap<T>(clazz);
	}
	
	public static <T> ConfigList<T> list(Class<T> clazz, T... elements) {
		ConfigList<T> list = new ConfigList<T>(clazz);
		for (T elem : elements) {
			list.add(elem);
		}
		return list;
	}
	
	private YamlConfiguration config;
	private File file = null;
	private Object data = null;
	private boolean registered = false;
	private List<ConfigField> fields = new ArrayList<>();
	protected Map<Class<?>, TypeConverter<?>> converters = new HashMap<>();
	
	/**
	 * Instantiates a ConfigManager with the default config name config.yml in the plugin's data folder
	 * @param plugin The plugin
	 */
	public ConfigManager(Plugin plugin) {
		this(plugin, "config.yml");
	}
	
	/**
	 * Instantiates a ConfigManager with a specific config name in the plugin's config folder
	 * @param plugin The plugin
	 * @param name The name of the config file to generate
	 */
	public ConfigManager(Plugin plugin, String name) {
		this(new File(plugin.getDataFolder(), name));
	}
	
	/**
	 * Initiates a ConfigManager using a specific file for the config
	 * @param file The file
	 */
	public ConfigManager(File file) {
		this.file = file;
		file.getParentFile().mkdirs();
		if (file.exists()) {
			config = YamlConfiguration.loadConfiguration(file);
		} else {
			config = new YamlConfiguration();
		}
	}
	
	/**
	 * Adds a type converter, which will attempt to convert a String from config to another type that
	 * is not usually able to be stored in config
	 * @param clazz The class of the type
	 * @param load A function to convert from a string to the type
	 * @param save A function to convert from the type to a string
	 * @param <T> The type
	 * @return This ConfigManager
	 */
	public <T> ConfigManager addConverter(Class<T> clazz, Function<String, T> load, Function<T, String> save) {
		converters.put(clazz, new TypeConverter<T>(load, save));
		return this;
	}
	
	/**
	 * Initiates a ConfigManager using a specific path for the config
	 * @param path The path
	 */
	public ConfigManager(Path path) {
		this(path.toFile());
	}
	
	/**
	 * Register all the hooks for annotated fields in the the given object
	 * @param data The object to be registered
	 * @return This ConfigManager
	 */
	public ConfigManager register(Object data) {
		for (Field field : data.getClass().getDeclaredFields()) {
			ConfigHook hook = field.getAnnotation(ConfigHook.class);
			if (hook == null) {
				continue;
			}
			fields.add(new ConfigField(field, hook.value(), this));
		}
		this.data = data;
		registered = true;
		return this;
	}
	
	/**
	 * Registers all the hooks for static annotated fields in the given class
	 * @param clazz The class to be registered
	 * @return This ConfigManager
	 */
	public ConfigManager register(Class clazz) {
		for (Field field : clazz.getDeclaredFields()) {
			ConfigHook hook = field.getAnnotation(ConfigHook.class);
			if (hook == null) {
				continue;
			}
			fields.add(new ConfigField(field, hook.value(), this));
		}
		registered = true;
		return this;
	}
	
	/**
	 * @return Whether the config file exists
	 */
	public boolean configExists() {
		return file.exists();
	}
	
	/**
	 * Save default values - initial values in the hook fields - where they do not already exist in the config
	 * @return This ConfigManager
	 * @throws IllegalStateException if this ConfigManager has not been registered yet
	 */
	public ConfigManager saveDefaults() {
		if (!registered) {
			throw new IllegalStateException("Config manager is not registered");
		}
		fields.forEach(f -> f.saveIfAbsent(data, config));
		try {
			config.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	/**
	 * Loads all values from config into the annotated hook fields
	 * @return This ConfigManager
	 * @throws IllegalStateException if this ConfigManager has not been registered yet
	 */
	public ConfigManager load() {
		if (!registered) {
			throw new IllegalStateException("Config manager is not registered");
		}
		fields.forEach(f -> f.load(data, config));
		return this;
	}
	
	/**
	 * Saves all values from the annotated hook fields to config
	 * @return This ConfigManager
	 * @throws IllegalStateException if this ConfigManager has not been registered yet
	 */
	public ConfigManager save() {
		if (!registered) {
			throw new IllegalStateException("Config manager is not registered");
		}
		fields.forEach(f -> f.load(data, config));
		try {
			config.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}
	
}

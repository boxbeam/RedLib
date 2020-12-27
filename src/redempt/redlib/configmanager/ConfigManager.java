package redempt.redlib.configmanager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import redempt.redlib.configmanager.annotations.ConfigValue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * Loads config values into variables annotated with {@link ConfigValue}
 */
public class ConfigManager {
	
	/**
	 * Instantiates a List of Strings inline
	 * @param strings The Strings to include in the List
	 * @return The List of Strings
	 */
	public static List<String> stringList(String... strings) {
		List<String> list = new ArrayList<>();
		Collections.addAll(list, strings);
		return list;
	}
	
	/**
	 * Creates a ConfigMap from a given type. A ConfigMap extends LinkedHashMap. The class is not accessible, so
	 * store it in a HashMap variable. This method must be used to set the initial value for a variable
	 * which loads in a section from config.
	 * @param clazz The class of the value type
	 * @param <T> The value type
	 * @return An empty map of the given type, which will be populated when {@link ConfigManager#load()} is called
 	 */
	public static <T> ConfigMap<String, T> map(Class<T> clazz) {
		return map(clazz, ConversionType.AUTO);
	}
	
	/**
	 * Creates a ConfigMap from a given type. A ConfigMap extends LinkedHashMap. The class is not accessible, so
	 * store it in a HashMap variable. This method must be used to set the initial value for a variable
	 * which loads in a section from config.
	 * @param clazz The class of the value type
	 * @param type The method which will be used to convert the stored type
	 * @param <T> The value type
	 * @return An empty map of the given type, which will be populated when {@link ConfigManager#load()} is called
	 */
	public static <T> ConfigMap<String, T> map(Class<T> clazz, ConversionType type) {
		return new ConfigMap<>(String.class, clazz, type);
	}
	
	/**
	 * Creates a ConfigMap from a given type. A ConfigMap extends LinkedHashMap. The class is not accessible, so
	 * store it in a HashMap variable. This method must be used to set the initial value for a variable
	 * which loads in a section from config. The key class may only be a type which has converter from string to
	 * another type. For obvious reasons, it cannot be a config-mappable object.
	 * @param keyClass The class of the key type
	 * @param <K> The key type
	 * @param valueClass The class of the value type
	 * @param <V> The value type
	 * @return An empty map of the given type, which will be populated when {@link ConfigManager#load()} is called
	 */
	public static <K, V> ConfigMap<K, V> map(Class<K> keyClass, Class<V> valueClass) {
		return map(keyClass, valueClass, ConversionType.AUTO);
	}
	
	/**
	 * Creates a ConfigMap from a given type. A ConfigMap extends LinkedHashMap. The class is not accessible, so
	 * store it in a HashMap variable. This method must be used to set the initial value for a variable
	 * which loads in a section from config. The key class may only be a type which has converter from string to
	 * another type. For obvious reasons, it cannot be a config-mappable object.
	 * @param keyClass The class of the key type
	 * @param <K> The key type
	 * @param valueClass The class of the value type
	 * @param <V> The value type
	 * @param type The method which will be used to convert the value type
	 * @return An empty map of the given type, which will be populated when {@link ConfigManager#load()} is called
	 */
	public static <K, V> ConfigMap<K, V> map(Class<K> keyClass, Class<V> valueClass, ConversionType type) {
		return new ConfigMap<>(keyClass, valueClass, type);
	}
	
	/**
	 * Creates a ConfigList from a given type with initial elements. A ConfigList extends ArrayList. The class
	 * is not accessible, so store it in an ArrayList variable. This method must be used to set the initial
	 * value for a variable which loads a list from config using type converters.
	 * @param clazz The class of the type of the list
	 * @param elements The elements to initialize the list with
	 * @param <T> The type
	 * @return A list of the given type which has been populated with the given elements
	 */
	public static <T> ConfigList<T> list(Class<T> clazz, T... elements) {
		return list(clazz, ConversionType.AUTO, elements);
	}
	
	/**
	 * Creates a ConfigList from a given type with initial elements. A ConfigList extends ArrayList. The class
	 * is not accessible, so store it in an ArrayList variable. This method must be used to set the initial
	 * value for a variable which loads a list from config using type converters.
	 * @param clazz The class of the type of the list
	 * @param elements The elements to initialize the list with
	 * @param <T> The type
	 * @param type The method which will be used to convert the stored type
	 * @return A list of the given type which has been populated with the given elements
	 */
	public static <T> ConfigList<T> list(Class<T> clazz, ConversionType type, T... elements) {
		ConfigList<T> list = new ConfigList<T>(clazz, type);
		Collections.addAll(list, elements);
		return list;
	}
	
	private YamlConfiguration config;
	private File file = null;
	private Map<Object, List<ConfigField>> data = new HashMap<>();
	private boolean registered = false;
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
		converters.put(Integer.class, new TypeConverter<>(Integer::parseInt, Object::toString));
		converters.put(Long.class, new TypeConverter<>(Long::parseLong, Object::toString));
		converters.put(Double.class, new TypeConverter<>(Double::parseDouble, Object::toString));
		converters.put(Boolean.class, new TypeConverter<>(Boolean::valueOf, Object::toString));
		converters.put(Float.class, new TypeConverter<>(Float::parseFloat, Object::toString));
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
	 * Register all the hooks for annotated fields in the the given objects. Pass classes instead if static
	 * fields are used.
	 * @param data The object to be registered
	 * @return This ConfigManager
	 */
	public ConfigManager register(Object... data) {
		for (Object obj : data) {
			List<ConfigField> fields = new ArrayList<>();
			for (Field field : (obj instanceof Class ? ((Class) obj).getDeclaredFields() : obj.getClass().getDeclaredFields())) {
				ConfigValue hook = field.getAnnotation(ConfigValue.class);
				if (hook == null) {
					continue;
				}
				fields.add(new ConfigField(field, hook.value(), hook.priority(), this));
			}
			this.data.put(obj, fields);
			fields.sort(Comparator.comparingInt(f -> f.priority));
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
		data.forEach((k, v) -> {
			v.forEach(f -> f.saveIfAbsent(k, config));
		});
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
		config = YamlConfiguration.loadConfiguration(file);
		data.forEach((k, v) -> {
			v.forEach(f -> f.load(k, config));
		});
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
		data.forEach((k, v) -> {
			v.forEach(f -> f.save(k, config));
		});
		try {
			config.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	/**
	 * @return The configuration this ConfigManager is loading from and saving to
	 */
	public YamlConfiguration getConfig() {
		return config;
	}
	
}

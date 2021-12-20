package redempt.redlib.config.conversion;

import org.bukkit.configuration.ConfigurationSection;

/**
 * An interface which converts data in config for a given type
 * @param <T> The type
 * @author Redempt
 */
public interface TypeConverter<T> {
	
	/**
	 * Attemps to load the object from config
	 * @param section The ConfigurationSection to load from
	 * @param path The path to the data in the ConfigurationSection
	 * @param currentValue The current value, used for collections and maps
	 * @return The loaded object
	 */
	public T loadFrom(ConfigurationSection section, String path, T currentValue);
	
	/**
	 * Attemps to save the object to config
	 * @param t The object to save
	 * @param section The ConfigurationSection to save to
	 * @param path The path to the data that should be saved in the ConfigurationSection
	 */
	public void saveTo(T t, ConfigurationSection section, String path);
	
	/**
	 * Attemps to save the object to config
	 * @param t The object to save
	 * @param section The ConfigurationSection to save to
	 * @param path The path to the data that should be saved in the ConfigurationSection
	 * @param overwrite Whether to overwrite existing data
	 */
	public default void saveTo(T t, ConfigurationSection section, String path, boolean overwrite) {
		if (!overwrite && section.isSet(path)) {
			return;
		}
		saveTo(t, section, path);
	}

}
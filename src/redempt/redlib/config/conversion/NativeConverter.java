package redempt.redlib.config.conversion;

import org.bukkit.configuration.ConfigurationSection;

/**
 * A converter which saves and loads directly to config without modifying the data
 * @author Redempt
 */
public class NativeConverter {
	
	/**
	 * Creates a native type converter
	 * @param <T> The type
	 * @return A native type converter
	 */
	public static <T> TypeConverter<T> create() {
		return new TypeConverter<T>() {
			@Override
			public T loadFrom(ConfigurationSection section, String path, T currentValue) {
				return (T) section.get(path);
			}
			
			@Override
			public void saveTo(T t, ConfigurationSection section, String path) {
				section.set(path, t);
			}
		};
	}
	
}

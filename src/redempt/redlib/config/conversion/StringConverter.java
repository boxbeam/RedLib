package redempt.redlib.config.conversion;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.config.data.DataHolder;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A TypeConverter which can convert data directly to and from strings
 * @param <T> The type
 * @author Redempt
 */
public interface StringConverter<T> extends TypeConverter<T> {
	
	/**
	 * Creates a StringConverter using a loading function and a saving function
	 * @param loader A loading function which can convert from a string to the type
	 * @param saver A saving function which can convert from the type to a string
	 * @param <T> The type
	 * @return The created StringConverter
	 */
	public static <T> StringConverter<T> create(Function<String, T> loader, Function<T, String> saver) {
		return new StringConverter<T>() {
			@Override
			public T fromString(String str) {
				return loader.apply(str);
			}
			
			@Override
			public String toString(T t) {
				return saver.apply(t);
			}
		};
	}
	
	/**
	 * Converts from a string
	 * @param str The string
	 * @return The resulting object
	 */
	public T fromString(String str);
	
	/**
	 * Converts to a string
	 * @param t The object to convert
	 * @return The string representation
	 */
	public String toString(T t);
	
	/**
	 * @param section The ConfigurationSection to load from
	 * @param path The path to the data in the ConfigurationSection
	 * @param currentValue The current value, used for collections and maps
	 * @return
	 */
	@Override
	public default T loadFrom(DataHolder section, String path, T currentValue) {
		return fromString(section.getString(path));
	}
	
	/**
	 * @param t The object to save
	 * @param section The ConfigurationSection to save to
	 * @param path The path to the data that should be saved in the ConfigurationSection
	 */
	@Override
	public default void saveTo(T t, DataHolder section, String path, Map<String, List<String>> comments) {
		section.set(path, toString(t));
	}

}

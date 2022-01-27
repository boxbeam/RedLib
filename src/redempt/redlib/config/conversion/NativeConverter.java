package redempt.redlib.config.conversion;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.config.data.DataHolder;

import java.util.List;
import java.util.Map;

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
			public T loadFrom(DataHolder section, String path, T currentValue) {
				return (T) section.get(path);
			}
			
			@Override
			public void saveTo(T t, DataHolder section, String path) {
				section.set(path, t);
			}
		};
	}
	
}

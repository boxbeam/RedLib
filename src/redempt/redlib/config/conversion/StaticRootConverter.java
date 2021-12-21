package redempt.redlib.config.conversion;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.config.ConfigManager;
import redempt.redlib.config.ConfigType;
import redempt.redlib.config.data.DataHolder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A converter which saves to and loads from static fields and can only be used as a config target
 * @author Redempt
 */
public class StaticRootConverter {
	
	/**
	 * Creates a static root converter
	 * @param manager The ConfigManager handling the data
	 * @param root The class to save and load from non-transient static fields of
	 * @param <T> The type
	 * @return A static root converter
	 */
	public static <T> TypeConverter<T> create(ConfigManager manager, Class<?> root) {
		List<Field> fields = new ArrayList<>();
		Map<Field, TypeConverter<?>> converters = new HashMap<>();
		for (Field field : root.getDeclaredFields()) {
			int mod = field.getModifiers();
			if (Modifier.isTransient(mod) || !Modifier.isStatic(mod)) {
				continue;
			}
			field.setAccessible(true);
			fields.add(field);
			converters.put(field, manager.getConverter(ConfigType.get(field)));
		}
		return new TypeConverter<T>() {
			@Override
			public T loadFrom(DataHolder section, String path, T currentValue) {
				try {
					for (Field field : fields) {
						Object val = converters.get(field).loadFrom(section, field.getName(), null);
						field.set(null, val);
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				return null;
			}
			
			@Override
			public void saveTo(T t, DataHolder section, String path) {
				saveTo(t, section, path, true);
			}
			
			@Override
			public void saveTo(T t, DataHolder section, String path, boolean overwrite) {
				try {
					for (Field field : fields) {
						Object obj = field.get(null);
						saveWith(converters.get(field), obj, section, field.getName(), overwrite);
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		};
	}
	
	private static <T> void saveWith(TypeConverter<T> converter, Object obj, DataHolder section, String path, boolean overwrite) {
		converter.saveTo((T) obj, section, path, overwrite);
	}
	
}

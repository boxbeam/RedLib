package redempt.redlib.config.conversion;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.config.ConfigField;
import redempt.redlib.config.ConfigManager;
import redempt.redlib.config.ConfigType;
import redempt.redlib.config.data.DataHolder;
import redempt.redlib.config.instantiation.FieldSummary;

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
		FieldSummary summary = FieldSummary.getFieldSummary(manager, root, true);
		return new TypeConverter<T>() {
			@Override
			public T loadFrom(DataHolder section, String path, T currentValue) {
				for (ConfigField field : summary.getFields()) {
					Object val = summary.getConverters().get(field).loadFrom(section, field.getName(), null);
					field.set(val);
				}
				return null;
			}
			
			@Override
			public void saveTo(T t, DataHolder section, String path) {
				saveTo(t, section, path, true);
			}
			
			@Override
			public void saveTo(T t, DataHolder section, String path, boolean overwrite) {
				for (ConfigField field : summary.getFields()) {
					Object obj = field.get();
					saveWith(summary.getConverters().get(field), obj, section, field.getName(), overwrite);
				}
			}
		};
	}
	
	private static <T> void saveWith(TypeConverter<T> converter, Object obj, DataHolder section, String path, boolean overwrite) {
		converter.saveTo((T) obj, section, path, overwrite);
	}
	
}

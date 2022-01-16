package redempt.redlib.config.conversion;

import redempt.redlib.config.ConfigManager;
import redempt.redlib.config.ConfigType;
import redempt.redlib.config.data.DataHolder;

/**
 * A converter which can convert subclasses of mappable classes
 * @author Redempt
 */
public class SubclassConverter {
	
	/**
	 * Creates a TypeConverter that can convert subclasses
	 * @param manager The ConfigManager handling the data
	 * @param clazz The class to handle subclasses of
	 * @param isAbstract Whether the class is abstract or an interface
	 * @param <T> The type
	 * @return The converter
	 */
	public static <T> TypeConverter<T> create(ConfigManager manager, Class<T> clazz, boolean isAbstract) {
		TypeConverter<T> parent = !isAbstract ? ObjectConverter.create(manager, new ConfigType<>(clazz)) : null;
		return new TypeConverter<T>() {
			@Override
			public T loadFrom(DataHolder section, String path, T currentValue) {
				String typeName = section.getSubsection(path).getString("=type");
				if (typeName == null) {
					throw new IllegalStateException("Could not determine subclass for object with path " + path);
				}
				Class<?> type = manager.loadClass(typeName);
				if (!clazz.isAssignableFrom(type)) {
					throw new IllegalStateException(type + " is not a subclass of " + clazz);
				}
				TypeConverter<T> converter = type.equals(clazz) ? parent : (TypeConverter<T>) manager.getConverter(new ConfigType<>(type));
				return converter.loadFrom(section, path, currentValue);
			}
			
			@Override
			public void saveTo(T t, DataHolder section, String path) {
				Class<?> type = t.getClass();
				if (!clazz.isAssignableFrom(type)) {
					throw new IllegalStateException(type + " is not a subclass of " + clazz);
				}
				TypeConverter<T> converter = type.equals(clazz) ? parent : (TypeConverter<T>) manager.getConverter(new ConfigType<>(type));
				converter.saveTo(t, section, path);
				section.getSubsection(path).set("=type", type.getName());
			}
		};
	}
	
}

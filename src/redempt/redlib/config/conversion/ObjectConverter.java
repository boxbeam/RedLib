package redempt.redlib.config.conversion;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.config.ConfigManager;
import redempt.redlib.config.ConfigType;
import redempt.redlib.config.annotations.ConfigPath;
import redempt.redlib.config.annotations.ConfigPostInit;
import redempt.redlib.config.instantiation.InstantiationInfo;
import redempt.redlib.config.instantiation.Instantiator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A converter which builds objects from configuration sections
 * @author Redempt
 */
public class ObjectConverter {
	
	/**
	 * Creates an object converter
	 * @param manager The ConfigManager managing the data
	 * @param type The config type to convert
	 * @param <T> The type
	 * @return An object converter for the given type
	 */
	public static <T> TypeConverter<T> create(ConfigManager manager, ConfigType<?> type) {
		if (type.getType().isInterface() || Modifier.isAbstract(type.getType().getModifiers())) {
			throw new IllegalStateException("Cannot automatically convert abstract classe or interface " + type.getType());
		}
		List<Field> fields = new ArrayList<>();
		Map<Field, TypeConverter<?>> converters = new HashMap<>();
		Instantiator instantiator = Instantiator.getInstantiator(type.getType());
		Field configPath = null;
		StringConverter<?> configPathConverter = null;
		for (Field field : type.getType().getDeclaredFields()) {
			int mod = field.getModifiers();
			if (Modifier.isTransient(mod) || Modifier.isStatic(mod)) {
				continue;
			}
			field.setAccessible(true);
			if (field.isAnnotationPresent(ConfigPath.class)) {
				configPath = field;
				configPathConverter = manager.getStringConverter(ConfigType.get(configPath));
				continue;
			}
			fields.add(field);
			ConfigType<?> fieldType = ConfigType.get(field);
			converters.put(field, manager.getConverter(fieldType));
		}
		Method postInit = null;
		for (Method method : type.getType().getDeclaredMethods()) {
			int mod = method.getModifiers();
			if (Modifier.isStatic(mod)) {
				continue;
			}
			if (method.isAnnotationPresent(ConfigPostInit.class)) {
				if (method.getParameterCount() != 0) {
					throw new IllegalStateException("Post-init method must have no parameters: " + method);
				}
				method.setAccessible(true);
				postInit = method;
				break;
			}
		}
		InstantiationInfo info = new InstantiationInfo(postInit, configPath, configPathConverter);
		return new TypeConverter<T>() {
			@Override
			public T loadFrom(ConfigurationSection section, String path, T currentValue) {
				ConfigurationSection newSection = path == null ? section : section.getConfigurationSection(path);
				List<Object> objs = new ArrayList<>();
				for (Field field : fields) {
					Object value = converters.get(field).loadFrom(newSection, field.getName(), null);
					objs.add(value);
				}
				return (T) instantiator.instantiate(manager, currentValue, type.getType(), fields, objs, path, info);
			}
			
			@Override
			public void saveTo(T t, ConfigurationSection section, String path) {
				saveTo(t, section, path, true);
			}
			
			@Override
			public void saveTo(T t, ConfigurationSection section, String path, boolean overwrite) {
				if (path != null && section.isSet(path) && !overwrite) {
					return;
				}
				ConfigurationSection newSection = path == null ? section : section.createSection(path);
				try {
					for (Field field : fields) {
						saveWith(converters.get(field), field.get(t), newSection, field.getName(), overwrite);
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		};
	}
	
	private static <T> void saveWith(TypeConverter<T> converter, Object obj, ConfigurationSection section, String path, boolean overwrite) {
		converter.saveTo((T) obj, section, path, overwrite);
	}
	
}

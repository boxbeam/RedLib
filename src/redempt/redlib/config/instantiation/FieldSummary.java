package redempt.redlib.config.instantiation;

import redempt.redlib.config.ConfigField;
import redempt.redlib.config.ConfigManager;
import redempt.redlib.config.ConfigType;
import redempt.redlib.config.annotations.ConfigName;
import redempt.redlib.config.annotations.ConfigPath;
import redempt.redlib.config.annotations.ConfigPostInit;
import redempt.redlib.config.conversion.StringConverter;
import redempt.redlib.config.conversion.TypeConverter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a summary of the relevant fields, converters, and other info required to load objects from config
 * @author Redempt
 */
public class FieldSummary {
	
	/**
	 * Generates a FieldSummary of a class
	 * @param manager The ConfigManager with access to converters
	 * @param clazz The class being summarized
	 * @param staticContext Whether static fields should be retrieved instead of member fields
	 * @return A field summary
	 */
	public static FieldSummary getFieldSummary(ConfigManager manager, Class<?> clazz, boolean staticContext) {
		try {
			Field configPath = null;
			StringConverter<?> configPathConverter = null;
			List<ConfigField> fields = new ArrayList<>();
			Map<ConfigField, TypeConverter<?>> converters = new HashMap<>();
			Method postInit = null;
			for (Field field : clazz.getDeclaredFields()) {
				int mod = field.getModifiers();
				if (Modifier.isTransient(mod) || Modifier.isStatic(mod) != staticContext) {
					continue;
				}
				field.setAccessible(true);
				if (!staticContext && field.isAnnotationPresent(ConfigPath.class)) {
					configPath = field;
					configPathConverter = manager.getStringConverter(ConfigType.get(configPath));
					continue;
				}
				ConfigField cf = new ConfigField(field);
				fields.add(cf);
				converters.put(cf, manager.getConverter(ConfigType.get(field)));
			}
			
			if (!staticContext && Instantiator.isRecord(clazz)) {
				Constructor<?> constructor = clazz.getDeclaredConstructor(Arrays.stream(clazz.getDeclaredFields()).map(Field::getType).toArray(Class<?>[]::new));
				Parameter[] params = constructor.getParameters();
				int pos = 0;
				for (int i = 0; i < params.length; i++) {
					Parameter param = params[i];
					if (param.isAnnotationPresent(ConfigPath.class)) {
						continue;
					}
					ConfigName name = param.getAnnotation(ConfigName.class);
					if (name == null) {
						continue;
					}
					fields.get(pos).setName(name.value());
					pos++;
				}
			}
			
			if (!staticContext) {
				postInit = getPostInitMethod(clazz);
			}
			return new FieldSummary(fields, converters, configPath, configPathConverter, postInit);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static Method getPostInitMethod(Class<?> clazz) {
		for (Method method : clazz.getDeclaredMethods()) {
			int mod = method.getModifiers();
			if (!method.isAnnotationPresent(ConfigPostInit.class)) {
				continue;
			}
			if (Modifier.isStatic(mod) || Modifier.isAbstract(mod)) {
				throw new IllegalStateException("Post-init method may not be static or abstract: " + method.getName());
			}
			if (method.getParameterCount() != 0) {
				throw new IllegalStateException("Post-init method must have no arguments: " + method.getName());
			}
			method.setAccessible(true);
			return method;
		}
		return null;
	}
	
	private List<ConfigField> fields;
	private Map<ConfigField, TypeConverter<?>> converters;
	private Field configPath;
	private StringConverter<?> configPathConverter;
	private Method postInit;
	
	private FieldSummary(List<ConfigField> fields, Map<ConfigField, TypeConverter<?>> converters, Field configPath,
	                     StringConverter<?> configPathConverter, Method postInit) {
		this.fields = fields;
		this.converters = converters;
		this.configPath = configPath;
		this.configPathConverter = configPathConverter;
		this.postInit = postInit;
	}
	
	/**
	 * @return The ConfigFields that should be loaded to
	 */
	public List<ConfigField> getFields() {
		return fields;
	}
	
	/**
	 * @return The converters for all the field types
	 */
	public Map<ConfigField, TypeConverter<?>> getConverters() {
		return converters;
	}
	
	/**
	 * @return The ConfigPath field, if one exists
	 */
	public Field getConfigPath() {
		return configPath;
	}
	
	/**
	 * @return The converter for the ConfigPath field, if it exists
	 */
	public StringConverter<?> getConfigPathConverter() {
		return configPathConverter;
	}
	
	/**
	 * @return The post-init method, if it exists
	 */
	public Method getPostInit() {
		return postInit;
	}
	
}

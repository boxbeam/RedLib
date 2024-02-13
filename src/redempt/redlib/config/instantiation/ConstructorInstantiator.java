package redempt.redlib.config.instantiation;

import redempt.redlib.config.ConfigField;
import redempt.redlib.config.ConversionManager;
import redempt.redlib.config.annotations.ConfigPath;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.IntStream;

/**
 * An instantiator used for record types which passes in all necessary fields
 * @author Redempt
 */
public class ConstructorInstantiator implements Instantiator {

	/**
	 * Attempts to create an Instantiator for a record type, or a class which has a constructor taking all its fields
	 * in the same order they appear in the class
	 * @param clazz The class to create an Instantiator for
	 * @param <T> The type
	 * @return An Instantiator
	 */
	public static <T> Instantiator createDefault(Class<?> clazz) {
		try {
			Field[] fields = clazz.getDeclaredFields();
			Constructor<?> constructor = clazz.getDeclaredConstructor(Arrays.stream(fields).map(Field::getType).toArray(Class<?>[]::new));
			return new ConstructorInstantiator(constructor);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("Class '" + clazz.getName() + "' does not have a constructor that takes all of its fields in order");
		}
	}
	
	private Constructor<?> constructor;
	private Parameter[] params;
	
	private ConstructorInstantiator(Constructor<?> constructor) {
		this.constructor = constructor;
		params = constructor.getParameters();
	}

	private ConstructorInstantiator(Constructor<?> constructor, int[] indices) {
		this.constructor = constructor;
		params = constructor.getParameters();
	}
	
	/**
	 * Instantiates a new object using its constructor
	 * @param manager The ConversionManager handling converters
	 * @param target The target object, always ignored by this type of Instantiator
	 * @param clazz The class whose fields are being used
	 * @param values The values for the fields
	 * @param path The path in config
	 * @param info Extra info about the instantiation
	 * @param <T> The type
	 * @return The constructed object
	 */
	@Override
	public <T> T instantiate(ConversionManager manager, Object target, Class<T> clazz, List<Object> values, String path, FieldSummary info) {
		Object[] objs = new Object[params.length];
		int valuePos = 0;
		for (int i = 0; i < params.length; i++) {
			Parameter param = params[i];
			if (param.isAnnotationPresent(ConfigPath.class)) {
				objs[i] = info.getConfigPathConverter().fromString(path);
				continue;
			}
			objs[i] = values.get(valuePos);
			valuePos++;
		}
		try {
			return (T) constructor.newInstance(objs);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
}

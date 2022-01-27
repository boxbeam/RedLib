package redempt.redlib.config.instantiation;

import redempt.redlib.config.ConfigManager;
import redempt.redlib.config.ConversionManager;
import redempt.redlib.config.annotations.ConfigMappable;
import redempt.redlib.config.data.DataHolder;

import java.util.List;

/**
 * A utility to instantiate objects from values loaded from config
 * @author Redempt
 */
public interface Instantiator {
	
	public static boolean isRecord(Class<?> clazz) {
		return clazz.getSuperclass() != null && clazz.getSuperclass().getName().equals("java.lang.Record");
	}
	
	/**
	 * Attemps to get the appropriate Instantiator for the given class type
	 * @param clazz The class type
	 * @return An Instantiator
	 * @throws IllegalArgumentException If the class cannot be instantiated by known methods
	 */
	public static Instantiator getInstantiator(Class<?> clazz) {
		if (isRecord(clazz)) {
			return ConstructorInstantiator.create(clazz);
		}
		if (clazz.isAnnotationPresent(ConfigMappable.class)) {
			return new EmptyInstantiator();
		}
		throw new IllegalArgumentException("Cannot create instantiator for class which is not a record type and not annotated with ConfigMappable");
	}
	
	/**
	 * Instantiates and/or loads data into an object
	 * @param manager The ConversionManager handling converters
	 * @param target The target object, or null
	 * @param clazz The class whose fields are being used
	 * @param values The values for the fields
	 * @param path The path in config
	 * @param info Extra info about the instantiation
	 * @param <T> The type
	 * @return An instantiated object, or the input object with its fields modified
	 */
	public <T> T instantiate(ConversionManager manager, Object target, Class<T> clazz, List<Object> values, String path, FieldSummary info);
	
}

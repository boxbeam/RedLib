package redempt.redlib.config.instantiation;

import redempt.redlib.config.ConversionManager;
import redempt.redlib.config.annotations.ConfigConstructor;
import redempt.redlib.config.annotations.ConfigMappable;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A utility to instantiate objects from values loaded from config
 *
 * @author Redempt
 */
public interface Instantiator {

    public static boolean isRecord(Class<?> clazz) {
        return clazz.getSuperclass() != null && clazz.getSuperclass().getName().equals("java.lang.Record");
    }

    /**
     * Attemps to get the appropriate Instantiator for the given class type
     *
     * @param clazz The class type
     * @return An Instantiator
     * @throws IllegalArgumentException If the class cannot be instantiated by known methods
     */
    public static Instantiator getInstantiator(Class<?> clazz) {
        if (isRecord(clazz)) {
            return ConstructorInstantiator.createDefault(clazz);
        }
        if (clazz.isAnnotationPresent(ConfigMappable.class)) {
            Optional<Constructor<?>> constructor = Arrays.stream(clazz.getConstructors()).filter(c -> c.isAnnotationPresent(ConfigConstructor.class)).findFirst();
            return constructor.map(value -> ConstructorInstantiator.createDefault(clazz)).orElseGet(EmptyInstantiator::new);
        }
        throw new IllegalArgumentException("Cannot create instantiator for class which is not a record type and not annotated with ConfigMappable (" + clazz + ")");
    }

    /**
     * Instantiates and/or loads data into an object
     *
     * @param manager The ConversionManager handling converters
     * @param target  The target object, or null
     * @param clazz   The class whose fields are being used
     * @param values  The values for the fields
     * @param path    The path in config
     * @param info    Extra info about the instantiation
     * @param <T>     The type
     * @return An instantiated object, or the input object with its fields modified
     */
    public <T> T instantiate(ConversionManager manager, Object target, Class<T> clazz, List<Object> values, String path, FieldSummary info);

}

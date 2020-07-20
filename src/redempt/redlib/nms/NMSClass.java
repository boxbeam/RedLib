package redempt.redlib.nms;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps any class and provides methods for easy reflection
 * @author Redempt
 */
public class NMSClass {
	
	private Class<?> clazz;
	
	/**
	 * Constructs an NMSClass wrapping the given class
	 * @param clazz The class to wrap
	 */
	public NMSClass(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	/**
	 * @return The simple name of the wrapped class
	 */
	public String getName() {
		return clazz.getSimpleName();
	}
	
	/**
	 * @return The wrapped class
	 */
	public Class<?> getWrappedClass() {
		return clazz;
	}
	
	/**
	 * Calls a constructor of this class with the given arguments
	 * @param args The arguments to pass to the constructor
	 * @return An NMSObject wrapping the returned value
	 */
	public NMSObject getInstance(Object... args) {
		try {
			NMSHelper.unwrapArgs(args);
			return new NMSObject(clazz.getConstructor(NMSHelper.getArgTypes(args)).newInstance(args));
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Creates an array of this class type
	 * @param size The size of the array
	 * @return An NMSArray wrapping the array
	 */
	public NMSArray createArray(int size) {
		return new NMSArray(Array.newInstance(clazz, size));
	}
	
	/**
	 * Calls a static method of this class
	 * @param methodName The name of the static method
	 * @param args The arguments to pass to the static method
	 * @return An NMSObject wrapping the returned value from the method
	 */
	public NMSObject callStaticMethod(String methodName, Object... args) {
		try {
			Method method = NMSHelper.getMethod(clazz, methodName, NMSHelper.getArgTypes(args));
			return new NMSObject(method.invoke(null, args));
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Gets the value of a static field in the wrapped class
	 * @param name The name of the field
	 * @return An NMSObject wrapping the field
	 */
	public NMSObject getStaticField(String name) {
		try {
			Field field = clazz.getDeclaredField(name);
			field.setAccessible(true);
			return new NMSObject(field.get(null));
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
}

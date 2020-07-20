package redempt.redlib.nms;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps any Object and provides easy access to reflection methods
 * @author Redempt
 */
public class NMSObject {

	private Object obj;
	
	/**
	 * Constructs an NMSObject with the object it should wrap
	 * @param obj The object to wrap
	 */
	public NMSObject(Object obj) {
		this.obj = obj;
	}
	
	/**
	 * @return The wrapped object
	 */
	public Object getObject() {
		return obj;
	}
	
	/**
	 * @return The name of the class of the wrapped object
	 */
	public String getTypeName() {
		return obj.getClass().getSimpleName();
	}
	
	/**
	 * @return A wrapped NMSClass of the class of the wrapped object
	 */
	public NMSClass getType() {
		return new NMSClass(obj.getClass());
	}
	
	/**
	 * @return Whether this NMSObject is wrapping null
	 */
	public boolean isNull() {
		return obj == null;
	}
	
	/**
	 * Calls a method on the wrapped object
	 * @param name The name of the method
	 * @param args The arguments to pass to the method
	 * @return An NMSObject which is the returned value from the method
	 */
	public NMSObject callMethod(String name, Object... args) {
		try {
			Method method = NMSHelper.getMethod(obj.getClass(), name, NMSHelper.getArgTypes(args));
			return new NMSObject(method.invoke(obj, args));
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Gets the value stored in a field in the wrapped object
	 * @param name The name of the field
	 * @return A wrapped NMSObject with the value of the field
	 */
	public NMSObject getField(String name) {
		try {
			Field field = obj.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return new NMSObject(field.get(obj));
		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new IllegalArgumentException(e);
		}
	}

}

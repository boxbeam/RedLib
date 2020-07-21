package redempt.redlib.nms;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * A set of utility methods useful for handling NMS
 * @author Redempt
 */
public class NMSHelper {

	private static String packageName = null;
	
	/**
	 * Gets the full name of the NMS package
	 * @return The full name of the NMS package
	 */
	public static String getNMSPackage() {
		if (packageName == null) {
			for (Package pkg : Package.getPackages()) {
				if (pkg.getName().startsWith("net.minecraft.server.")) {
					packageName = pkg.getName();
					break;
				}
			}
		}
		return packageName;
	}
	
	/**
	 * @return The version section of the NMS package, like v1_15_R1
	 */
	public static String getNMSVersion() {
		String[] split = getNMSPackage().split("\\.");
		return split[split.length - 1];
	}
	
	/**
	 * Unwraps an array of arguments, replacing NMSObjects with their wrapped values
	 * @param args
	 */
	public static void unwrapArgs(Object... args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof NMSObject) {
				args[i] = ((NMSObject) args[i]).getObject();
			}
		}
	}
	
	/**
	 * Gets the class list of argument types for finding methods
	 * @param args The arguments to convert to their class types
	 * @return The class types of each argument
	 */
	public static Class<?>[] getArgTypes(Object... args) {
		unwrapArgs(args);
		Class<?>[] classes = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof NMSObject) {
				classes[i] = ((NMSObject) args[i]).getObject().getClass();
				continue;
			}
			classes[i] = args[i].getClass();
			switch (classes[i].getSimpleName()) {
				case "Integer":
					classes[i] = int.class;
					break;
				case "Boolean":
					classes[i] = boolean.class;
					break;
				case "Float":
					classes[i] = float.class;
					break;
				case "Double":
					classes[i] = double.class;
					break;
				case "Long":
					classes[i] = long.class;
					break;
				case "Short":
					classes[i] = short.class;
					break;
			}
		}
		return classes;
	}
	
	/**
	 * Gets a method by its name and parameter types, accounts for cases where the given class might not be the
	 * exact same type as the parameter the method requires, but is a subclass.
	 * @param clazz The class to get the method in
	 * @param name The name of the method
	 * @param argTypes The class types for the method parameters
	 * @return The method in the class, or null if none was found
	 */
	public static Method getMethod(Class<?> clazz, String name, Class<?>[] argTypes) {
		Method[] methods = clazz.getMethods();
		methods:
		for (Method method : methods) {
			if (!method.getName().equals(name) || method.getParameterCount() != argTypes.length) {
				continue;
			}
			Parameter[] params = method.getParameters();
			for (int i = 0; i < params.length; i++) {
				if (!params[i].getType().isAssignableFrom(argTypes[i])) {
					continue methods;
				}
			}
			return method;
		}
		return null;
	}
	
	/**
	 * Gets an NMS class (a class whose package is {@link net.minecraft.server} followed by the version package)
	 * by name.
	 * @param name The name of the class
	 * @return The NMSClass wrapping the resulting class
	 */
	public static NMSClass getNMSClass(String name) {
		try {
			return new NMSClass(Class.forName(getNMSPackage() + "." + name));
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Gets any class and wraps it in an NMSClass
	 * @param name The full name of the class
	 * @return The wrapped NMSClass
	 */
	public static NMSClass getClass(String name) {
		try {
			return new NMSClass(Class.forName(name));
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
}

package redempt.redlib.configmanager;

import redempt.redlib.configmanager.annotations.ConfigMappable;

/**
 * Represents ways in which objects can be mapped from config
 * @author Redempt
 */
public enum ConversionType {
	
	/**
	 * A config value mapped from a class annotated with {@link redempt.redlib.configmanager.annotations.ConfigMappable}
	 */
	MAPPED_OBJECT,
	/**
	 * A config value converted to and from a string using {@link TypeConverter}s
	 */
	STRING_CONVERTED,
	/**
	 * A config value loaded and stored directly from config
	 */
	PLAIN,
	/**
	 * Auto-detect the conversion type based on context
	 */
	AUTO;
	
	protected static ConversionType auto(Class<?> clazz, ConfigManager manager) {
		if (clazz.isAnnotationPresent(ConfigMappable.class)) {
			return ConversionType.MAPPED_OBJECT;
		}
		if (manager.getConverter(clazz) != null) {
			return ConversionType.STRING_CONVERTED;
		}
		return ConversionType.PLAIN;
	}

}

package redempt.redlib.config;

import redempt.redlib.config.annotations.ConfigName;

import java.lang.reflect.Field;

/**
 * Wraps a Field and stores the name which should be used to store its value in config
 * @author Redempt
 */
public class ConfigField {
	
	private Field field;
	private String name;
	
	/**
	 * Constructs a ConfigField from a field
	 * @param field The Field
	 */
	public ConfigField(Field field) {
		this.field = field;
		ConfigName annotation = field.getAnnotation(ConfigName.class);
		name = annotation == null ? field.getName() : annotation.value();
	}
	
	/**
	 * @return The wrapped Field
	 */
	public Field getField() {
		return field;
	}
	
	/**
	 * Attemps to set the value of the field for the target object to the value
	 * @param target The target object
	 * @param value The value
	 */
	public void set(Object target, Object value) {
		try {
			field.set(target, value);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Attemps to set the field in a static context to the given value
	 * @param value The value
	 */
	public void set(Object value) {
		set(null, value);
	}
	
	/**
	 * Attempts to get the field's value for a given object
	 * @param target The target object to get the value from
	 * @return The value
	 */
	public Object get(Object target) {
		try {
			return field.get(target);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Attemps to get the value of the field in a static context
	 * @return The value
	 */
	public Object get() {
		return get(null);
	}
	
	/**
	 * @return The name for the field that should be used to store config values
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of this ConfigField
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	public int hashCode() {
		return field.hashCode();
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof ConfigField)) {
			return false;
		}
		ConfigField cf = (ConfigField) o;
		return cf.field.equals(field);
	}
	
}

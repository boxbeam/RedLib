package redempt.redlib.configmanager.annotations;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
/**
 * Used to indicate that a field should be saved to and loaded from config
 */
public @interface ConfigValue {
	
	/**
	 * @return The path to the config value represented by the field bearing this annotation
	 */
	String value();
	int priority() default 1;
	
}

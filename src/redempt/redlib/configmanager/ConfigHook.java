package redempt.redlib.configmanager;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigHook {
	
	/**
	 * @return The path to the config value represented by the field bearing this annotation
	 */
	public String value();
	
}

package redempt.redlib.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to denote comments which should be applied to a config path. Only supported in 1.18.1+
 * @author Redempt
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Repeatable(Comments.class)
public @interface Comment {
	
	String value();
	
}

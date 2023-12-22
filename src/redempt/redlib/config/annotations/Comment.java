package redempt.redlib.config.annotations;

import java.lang.annotation.*;

/**
 * Used to denote comments which should be applied to a config path. Only supported in 1.18.1+
 * @author Redempt
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Repeatable(Comments.class)
@Inherited
public @interface Comment {
	
	String value();
	
}

package redempt.redlib.config.annotations;

import java.lang.annotation.*;

/**
 * Indicates that a method should be run when an object is deserialized from config
 * @author Redempt
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ConfigPostInit {
}

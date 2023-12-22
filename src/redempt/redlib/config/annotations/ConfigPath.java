package redempt.redlib.config.annotations;

import java.lang.annotation.*;

/**
 * Indicates that a field should be populated with the last element of the path to its location in config
 * @author Redempt
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ConfigPath {
}

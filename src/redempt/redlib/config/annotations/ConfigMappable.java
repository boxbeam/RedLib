package redempt.redlib.config.annotations;

import java.lang.annotation.*;

/**
 * Indicates that a class can be automatically serialized to config
 *
 * @author Redempt
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigMappable {
}

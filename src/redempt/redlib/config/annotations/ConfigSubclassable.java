package redempt.redlib.config.annotations;

import java.lang.annotation.*;

/**
 * Indicates that this type can be subclassed and stored in config, its type will be stored alongside its metadata
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface ConfigSubclassable {
}

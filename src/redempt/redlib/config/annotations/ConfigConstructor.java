package redempt.redlib.config.annotations;

import java.lang.annotation.*;

/**
 * Denotes a constructor which can be used to initialize the object from config.
 * Parameter names in the constructor must match field names in the class.
 */
@Inherited
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigConstructor {
}

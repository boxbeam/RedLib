package redempt.redlib.config.annotations;

import java.lang.annotation.*;

/**
 * Specifies the name that should be used to access and set a value in config
 *
 * @author Redempt
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ConfigName {

    String value();

}

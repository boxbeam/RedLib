package redempt.redlib.config.annotations;

import java.lang.annotation.*;

/**
 * A wrapper for multiple {@link Comment} annotations
 *
 * @author Redempt
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Inherited
public @interface Comments {

    Comment[] value();

}

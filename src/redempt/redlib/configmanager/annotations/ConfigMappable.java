package redempt.redlib.configmanager.annotations;

import java.lang.annotation.*;

/**
 * Used to indicate that a class can be mapped to and from config and has fields annotated with {@link ConfigValue}
 * @deprecated Old API
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ConfigMappable {}

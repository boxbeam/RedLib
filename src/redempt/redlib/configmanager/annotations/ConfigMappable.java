package redempt.redlib.configmanager.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
/**
 * Used to indicate that a class can be mapped to and from config and has fields annotated with {@link ConfigValue}
 */
public @interface ConfigMappable {}

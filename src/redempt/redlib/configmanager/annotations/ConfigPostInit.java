package redempt.redlib.configmanager.annotations;

import java.lang.annotation.*;

/**
 * Denotes a method to be run after an object's fields have been fully loaded by a ConfigMap
 * @deprecated Old API
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface ConfigPostInit {}

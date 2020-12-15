package redempt.redlib.configmanager.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
/**
 * Denotes a method to be run after an object's fields have been fully loaded by a ConfigMap
 */
public @interface ConfigPostInit {}

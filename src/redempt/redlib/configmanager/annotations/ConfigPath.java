package redempt.redlib.configmanager.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
/**
 * Used to denote that the field should be populated with the path to the configuration section.
 * If the field is a String, it will be populated with the name of the section. Otherwise, it will be
 * populated with the {@link org.bukkit.configuration.ConfigurationSection}.
 */
public @interface ConfigPath {}

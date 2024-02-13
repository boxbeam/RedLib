package redempt.redlib.config.instantiation;

import redempt.redlib.config.ConfigField;
import redempt.redlib.config.ConfigType;
import redempt.redlib.config.ConversionManager;
import redempt.redlib.config.annotations.*;
import redempt.redlib.config.conversion.StringConverter;
import redempt.redlib.config.conversion.TypeConverter;
import redempt.redlib.config.data.DataHolder;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a summary of the relevant fields, converters, and other info required to load objects from config
 *
 * @author Redempt
 */
public class FieldSummary {

    /**
     * Gets the comments applied to a field
     *
     * @param field The Field
     * @return The comments applied to the field
     */
    public static List<String> getComments(Field field) {
        Comments comments = field.getAnnotation(Comments.class);
        if (comments == null) {
            Comment comment = field.getAnnotation(Comment.class);
            return comment == null ? null : Collections.singletonList(comment.value());
        }
        return Arrays.stream(comments.value()).map(Comment::value).collect(Collectors.toList());
    }

    /**
     * Gets the comments applied to a parameter
     *
     * @param param The Parameter
     * @return The comments applied to the parameter
     */
    public static List<String> getComments(Parameter param) {
        Comments comments = param.getAnnotation(Comments.class);
        if (comments == null) {
            Comment comment = param.getAnnotation(Comment.class);
            return comment == null ? null : Collections.singletonList(comment.value());
        }
        return Arrays.stream(comments.value()).map(Comment::value).collect(Collectors.toList());
    }

    /**
     * Generates a FieldSummary of a class
     *
     * @param manager       The ConversionManager with access to converters
     * @param clazz         The class being summarized
     * @param staticContext Whether static fields should be retrieved instead of member fields
     * @return A field summary
     */
    public static FieldSummary getFieldSummary(ConversionManager manager, Class<?> clazz, boolean staticContext) {
        try {
            Field configPath = null;
            StringConverter<?> configPathConverter = null;
            List<ConfigField> fields = new ArrayList<>();
            Map<ConfigField, TypeConverter<?>> converters = new HashMap<>();
            Method postInit = null;
            while (clazz != null && (staticContext || clazz.isAnnotationPresent(ConfigMappable.class) || Instantiator.isRecord(clazz))) {
                for (Field field : clazz.getDeclaredFields()) {
                    int mod = field.getModifiers();
                    if (field.isSynthetic() || Modifier.isTransient(mod) || Modifier.isStatic(mod) != staticContext) {
                        continue;
                    }
                    field.setAccessible(true);
                    if (!staticContext && field.isAnnotationPresent(ConfigPath.class)) {
                        configPath = field;
                        configPathConverter = manager.getStringConverter(ConfigType.get(configPath));
                        continue;
                    }
                    ConfigField cf = new ConfigField(field);
                    fields.add(cf);
                    converters.put(cf, manager.getConverter(ConfigType.get(field)));
                }

                if (!staticContext && Instantiator.isRecord(clazz)) {
                    Constructor<?> constructor = clazz.getDeclaredConstructor(Arrays.stream(clazz.getDeclaredFields()).map(Field::getType).toArray(Class<?>[]::new));
                    Parameter[] params = constructor.getParameters();
                    int pos = 0;
                    for (int i = 0; i < params.length; i++) {
                        Parameter param = params[i];
                        if (param.isAnnotationPresent(ConfigPath.class)) {
                            continue;
                        }
                        fields.get(pos).setComments(getComments(param));
                        ConfigName name = param.getAnnotation(ConfigName.class);
                        if (name == null) {
                            continue;
                        }
                        fields.get(pos).setName(name.value());
                        pos++;
                    }
                }
                if (!staticContext && postInit == null) {
                    postInit = getPostInitMethod(clazz);
                }
                clazz = clazz.getSuperclass();
            }
            return new FieldSummary(fields, converters, configPath, configPathConverter, postInit);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Method getPostInitMethod(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            int mod = method.getModifiers();
            if (!method.isAnnotationPresent(ConfigPostInit.class)) {
                continue;
            }
            if (Modifier.isStatic(mod) || Modifier.isAbstract(mod)) {
                throw new IllegalStateException("Post-init method may not be static or abstract: " + method.getName());
            }
            if (method.getParameterCount() != 0) {
                throw new IllegalStateException("Post-init method must have no arguments: " + method.getName());
            }
            method.setAccessible(true);
            return method;
        }
        return null;
    }

    private List<ConfigField> fields;
    private Map<ConfigField, TypeConverter<?>> converters;
    private Field configPath;
    private StringConverter<?> configPathConverter;
    private Method postInit;

    private FieldSummary(List<ConfigField> fields, Map<ConfigField, TypeConverter<?>> converters, Field configPath,
                         StringConverter<?> configPathConverter, Method postInit) {
        this.fields = fields;
        this.converters = converters;
        this.configPath = configPath;
        this.configPathConverter = configPathConverter;
        this.postInit = postInit;
    }

    /**
     * @return The ConfigFields that should be loaded to
     */
    public List<ConfigField> getFields() {
        return fields;
    }

    /**
     * @return The converters for all the field types
     */
    public Map<ConfigField, TypeConverter<?>> getConverters() {
        return converters;
    }

    /**
     * @return The ConfigPath field, if one exists
     */
    public Field getConfigPath() {
        return configPath;
    }

    /**
     * @return The converter for the ConfigPath field, if it exists
     */
    public StringConverter<?> getConfigPathConverter() {
        return configPathConverter;
    }

    /**
     * @return The post-init method, if it exists
     */
    public Method getPostInit() {
        return postInit;
    }

    /**
     * Attempts to apply comments to the given DataHolder
     *
     * @param holder The DataHolder to apply comments to
     */
    public void applyComments(DataHolder holder) {
        fields.forEach(f -> {
            if (f.getComments() != null) {
                holder.setComments(f.getName(), f.getComments());
            }
        });
    }

}

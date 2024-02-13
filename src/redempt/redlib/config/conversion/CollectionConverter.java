package redempt.redlib.config.conversion;

import redempt.redlib.config.ConfigType;
import redempt.redlib.config.ConversionManager;
import redempt.redlib.config.data.DataHolder;
import redempt.redlib.config.data.ListDataHolder;

import java.util.*;
import java.util.function.Supplier;

/**
 * A converter which saves and loads contents of a collection
 *
 * @author Redempt
 */
public class CollectionConverter {

    private static Map<Class<?>, Supplier<? extends Collection<?>>> defaults;

    static {
        defaults = new HashMap<>();
        defaults.put(List.class, ArrayList::new);
        defaults.put(Set.class, LinkedHashSet::new);
        defaults.put(Queue.class, ArrayDeque::new);
    }

    /**
     * Creates a collection converter
     *
     * @param manager        The ConfigManager handling the data
     * @param collectionType The ConfigType of the collection with full generic info
     * @param <V>            The component type of the collection
     * @param <T>            The collection type
     * @return A collection converter
     */
    public static <V, T extends Collection<V>> TypeConverter<T> create(ConversionManager manager, ConfigType<?> collectionType) {
        ConfigType<?> componentType = collectionType.getComponentTypes().get(0);
        TypeConverter<V> converter = (TypeConverter<V>) manager.getConverter(componentType);
        return new TypeConverter<T>() {
            @Override
            public T loadFrom(DataHolder section, String path, T currentValue) {
                if (currentValue == null) {
                    currentValue = (T) defaults.get(collectionType.getType()).get();
                } else {
                    currentValue.clear();
                }
                DataHolder newSection = path == null ? section : section.getList(path);
                if (newSection == null) {
                    return null;
                }
                T collection = currentValue;
                newSection.getKeys().forEach(k -> {
                    V obj = converter.loadFrom(newSection, k, null);
                    collection.add(obj);
                });
                return collection;
            }

            @Override
            public void saveTo(T t, DataHolder section, String path) {
                DataHolder newSection = new ListDataHolder();
                int pos = 0;
                if (t != null) {
                    for (V obj : t) {
                        converter.saveTo(obj, newSection, String.valueOf(pos));
                        pos++;
                    }
                }
                section.set(path, newSection);
            }
        };
    }

}

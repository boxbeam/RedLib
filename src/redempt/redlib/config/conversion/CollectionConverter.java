package redempt.redlib.config.conversion;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.config.ConfigManager;
import redempt.redlib.config.ConfigType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A converter which saves and loads contents of a collection
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
	 * @param manager The ConfigManager handling the data
	 * @param collectionType The ConfigType of the collection with full generic info
	 * @param <V> The component type of the collection
	 * @param <T> The collection type
	 * @return A collection converter
	 */
	public static <V, T extends Collection<V>> TypeConverter<T> create(ConfigManager manager, ConfigType<?> collectionType) {
		TypeConverter<V> converter = (TypeConverter<V>) manager.getConverter(collectionType.getComponentTypes().get(0));
		return new TypeConverter<T>() {
			@Override
			public T loadFrom(ConfigurationSection section, String path, T currentValue) {
				if (currentValue == null) {
					currentValue = (T) defaults.get(collectionType.getType()).get();
				} else {
					currentValue.clear();
				}
				ConfigurationSection newSection = section.getConfigurationSection(path);
				if (newSection == null) {
					return null;
				}
				T collection = currentValue;
				newSection.getKeys(false).forEach(k -> {
					collection.add(converter.loadFrom(newSection, k, null));
				});
				return collection;
			}
			
			@Override
			public void saveTo(T t, ConfigurationSection section, String path) {
				ConfigurationSection newSection = section.createSection(path);
				int pos = 0;
				for (V obj : t) {
					converter.saveTo(obj, newSection, String.valueOf(pos));
					pos++;
				}
			}
		};
	}
	
}

package redempt.redlib.config.conversion;

import redempt.redlib.config.ConfigType;
import redempt.redlib.config.ConversionManager;
import redempt.redlib.config.data.DataHolder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A converter which saves and loads contents of a map
 * @author Redempt
 */
public class MapConverter {
	
	/**
	 * Creates a MapConverter
	 * @param manager The ConversionManager handling converters
	 * @param type The ConfigType of the map with complete generic information
	 * @param <K> The key type of the map
	 * @param <V> The value type of the map
	 * @param <M> The map type
	 * @return A MapConverter for the given type
	 */
	public static <K, V, M extends Map<K, V>> TypeConverter<M> create(ConversionManager manager, ConfigType<?> type) {
		List<ConfigType<?>> types = type.getComponentTypes();
		StringConverter<K> keyConverter = (StringConverter<K>) manager.getStringConverter(types.get(0));
		TypeConverter<V> valueConverter = (TypeConverter<V>) manager.getConverter(types.get(1));
		return new TypeConverter<M>() {
			@Override
			public M loadFrom(DataHolder section, String path, M currentValue) {
				if (currentValue == null) {
					currentValue = (M) new LinkedHashMap<>();
				} else {
					currentValue.clear();
				}
				M map = currentValue;
				DataHolder newSection = path == null ? section : section.getSubsection(path);
				if (newSection == null) {
					return null;
				}
				newSection.getKeys().forEach(k -> {
					K key = keyConverter.fromString(k);
					V value = valueConverter.loadFrom(newSection, k, null);
					map.put(key, value);
				});
				return map;
			}
			
			@Override
			public void saveTo(M m, DataHolder section, String path) {
				DataHolder newSection = path == null ? section : section.createSubsection(path);
				if (m == null) {
					return;
				}
				m.forEach((k, v) -> {
					String keyPath = keyConverter.toString(k);
					valueConverter.saveTo(v, newSection, keyPath);
				});
			}
		};
	}
	
}

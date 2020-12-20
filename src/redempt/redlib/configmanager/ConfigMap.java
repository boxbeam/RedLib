package redempt.redlib.configmanager;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.configmanager.annotations.ConfigMappable;
import redempt.redlib.configmanager.exceptions.ConfigMapException;

import java.util.LinkedHashMap;

class ConfigMap<K, V> extends LinkedHashMap<K, V> implements ConfigStorage {
	
	protected Class<V> valueClass;
	private Class<K> keyClass;
	protected ConfigurationSection section;
	private ConfigManager manager;
	private ConfigObjectMapper<V> mapper;
	private TypeConverter<K> converter;
	
	public ConfigMap(Class<K> keyClass, Class<V> valueClass) {
		this.valueClass = valueClass;
		this.keyClass = keyClass;
	}
	
	public void init(ConfigManager manager) {
		if (this.manager != null) {
			return;
		}
		if (valueClass.isAnnotationPresent(ConfigMappable.class)) {
			mapper = new ConfigObjectMapper<>(valueClass, manager);
		}
		this.manager = manager;
	}
	
	public void load(ConfigurationSection section) {
		clear();
		this.section = section;
		if (valueClass.isAnnotationPresent(ConfigMappable.class)) {
			section.getKeys(false).forEach(k -> {
				put(getObjKey(k), mapper.load(section.getConfigurationSection(k)));
			});
			return;
		}
		TypeConverter<V> converter = (TypeConverter<V>) manager.converters.get(valueClass);
		if (converter == null) {
			section.getKeys(false).forEach(k -> {
				put(getObjKey(k), (V) section.get(k));
			});
			return;
		}
		section.getKeys(false).forEach(k -> {
			put(getObjKey(k), converter.load(section.getString(k)));
		});
	}
	
	public void save(ConfigurationSection section) {
		this.section = section;
		if (valueClass.isAnnotationPresent(ConfigMappable.class)) {
			forEach((k, v) -> {
				ConfigurationSection sect = section.createSection(getStringKey(k));
				mapper.save(sect, v);
			});
			return;
		}
		TypeConverter<V> converter = (TypeConverter<V>) manager.converters.get(valueClass);
		if (converter == null) {
			forEach((k, v) -> {
				section.set(getStringKey(k), v);
			});
			return;
		}
		forEach((k, v) -> {
			section.set(getStringKey(k), converter.save(v));
		});
	}
	
	@Override
	public V put(K key, V value) {
		V out = super.put(key, value);
		if (section != null && mapper != null) {
			String skey = getStringKey(key);
			ConfigurationSection section = this.section.getConfigurationSection(skey);
			section = section == null ? this.section.createSection(skey) : section;
			mapper.setPathField(value, section);
		}
		return out;
	}
	
	private String getStringKey(K key) {
		if (keyClass.equals(String.class)) {
			return (String) key;
		}
		if (converter == null) {
			converter = (TypeConverter<K>) manager.converters.get(keyClass);
			if (converter == null) {
				throw new ConfigMapException("No converter for class " + keyClass.getName());
			}
		}
		return converter.save(key);
	}
	
	private K getObjKey(String key) {
		if (keyClass.equals(String.class)) {
			return (K) key;
		}
		if (converter == null) {
			converter = (TypeConverter<K>) manager.converters.get(keyClass);
			if (converter == null) {
				throw new ConfigMapException("No converter for class " + keyClass.getName());
			}
		}
		return converter.load(key);
	}
	
}

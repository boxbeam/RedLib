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
	private ConversionType type;
	
	public ConfigMap(Class<K> keyClass, Class<V> valueClass, ConversionType type) {
		this.valueClass = valueClass;
		this.keyClass = keyClass;
		this.type = type;
	}
	
	public void init(ConfigManager manager) {
		if (this.manager != null) {
			return;
		}
		mapper = new ConfigObjectMapper<V>(valueClass, type, manager);
		this.manager = manager;
	}
	
	public void load(ConfigurationSection section) {
		clear();
		this.section = section;
		section.getKeys(false).forEach(k -> {
			super.put(getObjKey(k), mapper.load(section, k));
		});
	}
	
	public void save(ConfigurationSection section) {
		this.section = section;
		forEach((k, v) -> {
			mapper.save(section, getStringKey(k), v);
		});
	}
	
	@Override
	public V put(K key, V value) {
		V out = super.put(key, value);
		if (section != null) {
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
			converter = manager.getConverter(keyClass);
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
			converter = manager.getConverter(keyClass);
			if (converter == null) {
				throw new ConfigMapException("No converter for class " + keyClass.getName());
			}
		}
		return converter.load(key);
	}
	
}

package redempt.redlib.configmanager;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.configmanager.annotations.ConfigMappable;
import redempt.redlib.configmanager.exceptions.ConfigMapException;

import java.util.ArrayList;

class ConfigList<T> extends ArrayList<T> implements ConfigStorage {
	
	protected Class<T> clazz;
	private ConfigObjectMapper<T> mapper;
	private ConfigurationSection section;
	private ConfigManager manager;
	
	public ConfigList(Class<T> clazz) {
		this.clazz = clazz;
	}
	
	@Override
	public void init(ConfigManager manager) {
		if (this.manager != null) {
			return;
		}
		if (clazz.isAnnotationPresent(ConfigMappable.class)) {
			mapper = new ConfigObjectMapper<>(clazz, manager);
		}
		this.manager = manager;
		
	}
	
	@Override
	public void save(ConfigurationSection section) {
		int[] count = {0};
		if (clazz.isAnnotationPresent(ConfigMappable.class)) {
			forEach(i -> {
				ConfigurationSection sect = section.createSection(count[0] + "");
				count[0]++;
				mapper.save(sect, i);
			});
			return;
		}
		TypeConverter<T> converter = (TypeConverter<T>) manager.converters.get(clazz);
		if (converter == null) {
			throw new ConfigMapException("No converter for class " + clazz.getName());
		}
		forEach(i -> {
			section.set(count[0] + "", converter.save(i));
			count[0]++;
		});
	}
	
	@Override
	public void load(ConfigurationSection section) {
		if (clazz.isAnnotationPresent(ConfigMappable.class)) {
			section.getKeys(false).forEach(k -> {
				add(mapper.load(section.getConfigurationSection(k)));
			});
			return;
		}
		TypeConverter<T> converter = (TypeConverter<T>) manager.converters.get(clazz);
		if (converter == null) {
			throw new ConfigMapException("No converter for class " + clazz.getName());
		}
		section.getKeys(false).forEach(k -> {
			add(converter.load(section.getString(k)));
		});
	}
	
}

package redempt.redlib.configmanager;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.configmanager.exceptions.ConfigMapException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

class ConfigMap<T> extends HashMap<String, T> {
	
	private Class<T> clazz;
	protected ConfigurationSection section;
	private ConfigManager manager;
	private List<ConfigField> fields = new ArrayList<>();
	
	public ConfigMap(Class<T> clazz) {
		this.clazz = clazz;
	}
	
	public void init(ConfigManager manager) {
		if (fields.size() > 0) {
			return;
		}
		this.manager = manager;
		for (Field field : clazz.getDeclaredFields()) {
			ConfigHook hook = field.getAnnotation(ConfigHook.class);
			if (hook == null) {
				continue;
			}
			fields.add(new ConfigField(field, hook.value(), manager));
		}
	}
	
	public void load() {
		clear();
		for (String key : section.getKeys(false)) {
			ConfigurationSection section = this.section.getConfigurationSection(key);
			try {
				Constructor<T> constructor = clazz.getDeclaredConstructor();
				constructor.setAccessible(true);
				T instance = constructor.newInstance();
				fields.forEach(f -> f.load(instance, section));
				put(key, instance);
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
				throw new ConfigMapException("Class must have a default constructor with no arguments!");
			}
		}
		ConfigManager.postInit.forEach(Runnable::run);
		ConfigManager.postInit.clear();
	}
	
	public void save() {
		forEach((k, v) -> {
			ConfigurationSection section = this.section.getConfigurationSection(k);
			section = section == null ? this.section.createSection(k) : section;
			ConfigurationSection fsection = section;
			fields.forEach(f -> f.save(v, fsection));
		});
	}
	
	@Override
	public T remove(Object key) {
		if (key instanceof String) {
			section.set((String) key, null);
		}
		return super.remove(key);
	}
	
	@Override
	public T put(String key, T value) {
		T out = super.put(key, value);
		fields.stream().filter(f -> f.getPath().equals("_section")).findFirst().ifPresent(f -> {
			ConfigurationSection section = this.section.getConfigurationSection(key);
			section = section == null ? this.section.createSection(key) : section;
			f.load(value, section);
		});
		return out;
	}
	
}

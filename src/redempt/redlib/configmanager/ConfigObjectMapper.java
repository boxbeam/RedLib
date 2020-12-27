package redempt.redlib.configmanager;

import org.bukkit.configuration.ConfigurationSection;
import redempt.redlib.configmanager.annotations.ConfigPath;
import redempt.redlib.configmanager.annotations.ConfigPostInit;
import redempt.redlib.configmanager.annotations.ConfigValue;
import redempt.redlib.configmanager.exceptions.ConfigMapException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class ConfigObjectMapper<T> {
	
	private Class<T> clazz;
	private List<ConfigField> fields = new ArrayList<>();
	private TypeConverter<T> converter;
	private Field pathField;
	private boolean pathFieldString = false;
	private Method postInit;
	private ConversionType type;
	
	public ConfigObjectMapper(Class<T> clazz, ConversionType type, ConfigManager manager) {
		this.clazz = clazz;
		if (type == ConversionType.AUTO) {
			type = ConversionType.auto(clazz, manager);
		}
		this.type = type;
		switch (type) {
			case MAPPED_OBJECT:
				for (Field field : clazz.getDeclaredFields()) {
					if (field.isAnnotationPresent(ConfigPath.class)) {
						pathField = field;
						pathField.setAccessible(true);
						if (field.getType().equals(String.class)) {
							pathFieldString = true;
						} else if (!field.getType().equals(ConfigurationSection.class)) {
							throw new ConfigMapException("Field annotated with @ConfigPath must be of type String or ConfigurationSection!");
						}
					}
					ConfigValue hook = field.getAnnotation(ConfigValue.class);
					if (hook == null) {
						continue;
					}
					fields.add(new ConfigField(field, hook.value(), hook.priority(), manager));
				}
				for (Method method : clazz.getDeclaredMethods()) {
					if (method.isAnnotationPresent(ConfigPostInit.class)) {
						if (method.getParameterCount() != 0) {
							throw new ConfigMapException("Post-init method must take no arguments");
						}
						postInit = method;
						postInit.setAccessible(true);
						break;
					}
				}
				fields.sort(Comparator.comparingInt(f -> f.priority));
				break;
			case STRING_CONVERTED:
				converter = (TypeConverter<T>) manager.converters.get(clazz);
				if (converter == null) {
					throw new ConfigMapException("No type converter for " + clazz.getName());
				}
				break;
		}

	}
	
	public T load(ConfigurationSection section, String path) {
		switch (type) {
			case MAPPED_OBJECT:
				return loadMapped(section.getConfigurationSection(path));
			case STRING_CONVERTED:
				return converter.load(section.getString(path));
			case PLAIN:
				return (T) section.get(path);
		}
		return null;
	}
	
	private T loadMapped(ConfigurationSection section) {
		try {
			Constructor<T> constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible(true);
			T inst = constructor.newInstance();
			fields.forEach(f -> f.load(inst, section));
			setPathField(inst, section);
			if (postInit != null) {
				postInit.invoke(inst);
			}
			return inst;
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			throw new ConfigMapException("Class " + clazz.getName() + " must have a no-arg constructor");
		}
		return null;
	}
	
	public void save(ConfigurationSection section, String path, T inst) {
		switch (type) {
			case MAPPED_OBJECT:
				saveMapped(section.createSection(path), inst);
				break;
			case STRING_CONVERTED:
				section.set(path, converter.save(inst));
				break;
			case PLAIN:
				section.set(path, inst);
		}
	}
	
	private void saveMapped(ConfigurationSection section, T inst) {
		if (inst == null) {
			return;
		}
		fields.forEach(f -> f.save(inst, section));
	}
	
	public void setPathField(T inst, ConfigurationSection section) {
		if (pathField == null) {
			return;
		}
		try {
			pathField.set(inst, pathFieldString ? section.getName() : section);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
}

package redempt.redlib.configmanager;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;

class ConfigList<T> extends ArrayList<T> {
	
	protected Class<T> clazz;
	
	public ConfigList(Class<T> clazz) {
		this.clazz = clazz;
	}
	
	public void castAdd(Object o) {
		add((T) o);
	}
	
}

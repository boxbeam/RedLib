package redempt.redlib.config.data;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Set;

public class ConfigurationSectionDataHolder implements DataHolder {
	
	private ConfigurationSection section;
	
	public ConfigurationSectionDataHolder(ConfigurationSection section) {
		this.section = section;
	}
	
	@Override
	public Object get(String path) {
		return section.get(path);
	}
	
	@Override
	public void set(String path, Object obj) {
		section.set(path, DataHolder.unwrap(obj));
	}
	
	@Override
	public DataHolder getSubsection(String path) {
		ConfigurationSection subsection = section.getConfigurationSection(path);
		return subsection == null ? null : new ConfigurationSectionDataHolder(subsection);
	}
	
	@Override
	public DataHolder createSubsection(String path) {
		return new ConfigurationSectionDataHolder(section.createSection(path));
	}
	
	@Override
	public Set<String> getKeys() {
		return section.getKeys(false);
	}
	
	@Override
	public boolean isSet(String path) {
		return section.isSet(path);
	}
	
	@Override
	public String getString(String path) {
		return section.getString(path);
	}
	
	@Override
	public DataHolder getList(String path) {
		return new ListDataHolder(section.getList(path));
	}
	
	@Override
	public void remove(String path) {
		section.set(path, null);
	}
	
	@Override
	public Object unwrap() {
		return section;
	}
	
}

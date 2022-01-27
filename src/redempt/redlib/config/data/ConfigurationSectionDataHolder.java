package redempt.redlib.config.data;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigurationSectionDataHolder implements DataHolder {
	
	private ConfigurationSection section;
	private Map<String, List<String>> comments;
	
	public ConfigurationSectionDataHolder(ConfigurationSection section) {
		this(section, new HashMap<>());
	}
	
	private ConfigurationSectionDataHolder(ConfigurationSection section, Map<String, List<String>> comments) {
		this.section = section;
		this.comments = comments;
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
		return subsection == null ? null : new ConfigurationSectionDataHolder(subsection, comments);
	}
	
	@Override
	public DataHolder createSubsection(String path) {
		return new ConfigurationSectionDataHolder(section.createSection(path), comments);
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
	
	public void clearComments() {
		comments.clear();
	}
	
	public Map<String, List<String>> getComments() {
		return comments;
	}
	
	@Override
	public void setComments(String path, List<String> comments) {
		String key;
		String currentPath = section.getCurrentPath();
		if (currentPath == null || currentPath.equals(".")) {
			key = path;
		} else {
			key = currentPath + "." + path;
		}
		this.comments.put(key, comments);
	}
	
}

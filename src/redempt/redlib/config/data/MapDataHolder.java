package redempt.redlib.config.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapDataHolder implements DataHolder {
	
	private Map<String, Object> map;
	
	public MapDataHolder(Map<String, Object> map) {
		this.map = map;
	}
	
	public MapDataHolder() {
		this(new HashMap<>());
	}
	
	
	@Override
	public Object get(String path) {
		return map.get(path);
	}
	
	@Override
	public void set(String path, Object obj) {
		map.put(path, DataHolder.unwrap(obj));
	}
	
	@Override
	public DataHolder getSubsection(String path) {
		Object obj = map.get(path);
		return obj instanceof Map ? new MapDataHolder((Map<String, Object>) obj) : null;
	}
	
	@Override
	public DataHolder createSubsection(String path) {
		MapDataHolder subsection = new MapDataHolder();
		map.put(path, subsection.unwrap());
		return subsection;
	}
	
	@Override
	public Set<String> getKeys() {
		return map.keySet();
	}
	
	@Override
	public boolean isSet(String path) {
		return map.containsKey(path);
	}
	
	@Override
	public String getString(String path) {
		Object val = get(path);
		return val == null ? null : String.valueOf(val);
	}
	
	@Override
	public DataHolder getList(String path) {
		Object obj = map.get(path);
		return obj instanceof List ? new ListDataHolder((List<?>) obj) : null;
	}
	
	@Override
	public void remove(String path) {
		map.remove(path);
	}
	
	@Override
	public Object unwrap() {
		return map;
	}
	
}

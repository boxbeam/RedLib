package redempt.redlib.json;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Represents a list which can be serialized to JSON and deserialized back to this form,
 * assuming all of the values it stores are serializable
 */
public class JSONList extends ArrayList<Object> {
	
	public Integer getInt(int key) {
		Object o = get(key);
		if (o instanceof Long) {
			return (int) (long) o;
		}
		return (Integer) o;
	}
	
	public Boolean getBoolean(int key) {
		return (Boolean) get(key);
	}
	
	public Long getLong(int key) {
		return (Long) get(key);
	}
	
	public Double getDouble(int key) {
		return (Double) get(key);
	}
	
	public JSONList getList(int key) {
		return (JSONList) get(key);
	}
	
	public JSONMap getMap(int key) {
		return (JSONMap) get(key);
	}
	
	public String getString(int key) {
		return (String) get(key);
	}
	
	public <T> List<T> cast(Class<T> clazz) {
		return stream().map(clazz::cast).collect(Collectors.toList());
	}
	
	/**
	 * @return A JSON string representing this JSONList
	 */
	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(", ", "[", "]");
		forEach(o -> joiner.add(JSONParser.toJSONString(o)));
		return joiner.toString();
	}
	
}

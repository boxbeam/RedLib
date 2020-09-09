package redempt.redlib.json;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a list which can be serialized to JSON and deserialized back to this form,
 * assuming all of the values it stores are serializable
 */
public class JSONList extends ArrayList<Object> implements JSONStorage {
	
	private JSONStorage parent;
	protected String key = null;
	
	public int getInt(int key) {
		return (int) get(key);
	}
	
	public boolean getBoolean(int key) {
		return (boolean) get(key);
	}
	
	public long getLong(int key) {
		return (long) get(key);
	}
	
	public double getDouble(int key) {
		return (double) get(key);
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
		if (size() == 0) {
			return "[]";
		}
		StringBuilder builder = new StringBuilder("[");
		for (Object o : this) {
			if (o instanceof CharSequence) {
				builder.append('"').append(o.toString().replace("\\", "\\\\").replace("\"", "\\\"")).append("\", ");
				continue;
			}
			if (o instanceof Long) {
				builder.append(o.toString()).append("L, ");
				continue;
			}
			builder.append(o.toString()).append(", ");
		}
		return builder.replace(builder.length() - 2, builder.length(), "]").toString();
	}
	
	@Override
	public JSONStorage getParent() {
		return parent;
	}
	
	@Override
	public void setParent(JSONStorage obj) {
		this.parent = obj;
	}
	
	@Override
	public void add(String key, Object value) {
		add(value);
	}
	
	@Override
	public String getTempKey() {
		return key;
	}
	
	@Override
	public void setTempKey(String value) {
		this.key = value;
	}
	
}

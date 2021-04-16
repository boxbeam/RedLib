package redempt.redlib.json;

import java.util.HashMap;

/**
 * Represents a map which can be serialized to JSON and deserialized back to this form,
 * assuming all of the values it stores are serializable
 */
public class JSONMap extends HashMap<String, Object> implements JSONStorage {
	
	private JSONStorage parent;
	protected String key;
	
	public int getInt(String key) {
		Object o = get(key);
		return (int) (long) o;
	}
	
	public boolean getBoolean(String key) {
		return (boolean) get(key);
	}
	
	public double getDouble(String key) {
		return (double) get(key);
	}
	
	public long getLong(String key) {
		return (long) get(key);
	}
	
	public JSONList getList(String key) {
		return (JSONList) get(key);
	}
	
	public JSONMap getMap(String key) {
		return (JSONMap) get(key);
	}
	
	public String getString(String key) {
		return (String) get(key);
	}
	
	/**
	 * @return A JSON string representing this JSONMap
	 */
	@Override
	public String toString() {
		if (size() == 0) {
			return "{}";
		}
		StringBuilder builder = new StringBuilder("{");
		for (Entry<String, Object> entry : this.entrySet()) {
			builder.append('"').append(entry.getKey()).append('"').append(':');
			Object o = entry.getValue();
			if (o instanceof CharSequence) {
				builder.append('"').append((o.toString()).replace("\\", "\\\\").replace("\"", "\\\"")).append("\", ");
				continue;
			}
			if (o instanceof Long) {
				builder.append(o.toString()).append("L, ");
				continue;
			}
			builder.append(o).append(", ");
		}
		return builder.replace(builder.length() - 2, builder.length(), "}").toString();
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
		put(key, value);
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

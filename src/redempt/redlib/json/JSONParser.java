package redempt.redlib.json;

public class JSONParser {
	
	/**
	 * Parse a JSONList from a JSON string
	 * @param json The JSON string
	 * @return The JSONList parsed out of it
	 */
	public static JSONList parseList(String json) {
		return (JSONList) parse(json);
	}
	
	/**
	 * Parse a JSONMap from a JSON string
	 * @param json The JSON string
	 * @return TThe JSONList parsed out of it
	 */
	public static JSONMap parseMap(String json) {
		return (JSONMap) parse(json);
	}
	
	private static JSONStorage parse(String json) {
		Type parentType;
		switch (json.charAt(0)) {
			case '[':
				parentType = Type.LIST;
				break;
			case '{':
				parentType = Type.MAP;
				break;
			default:
				throw new IllegalArgumentException("Invalid JSON input");
		}
		boolean quoted = false;
		JSONStorage currentParent = parentType == Type.LIST ? new JSONList() : new JSONMap();
		JSONStorage root = currentParent;
		int cursor = 1;
		int lastChar = -1;
		boolean end = false;
		String key = null;
		Type type = Type.INT;
		char[] chars = json.toCharArray();
		for (int i = 1; i < json.length(); i++) {
			switch (chars[i]) {
				case ' ':
				case '\t':
				case '\n':
				case '\r':
					if (!quoted && lastChar == -1) {
						cursor = i + 1;
					}
					break;
				case '\\':
					i++;
					lastChar = i;
					break;
				case '"':
					quoted = !quoted;
					lastChar = i;
					type = Type.STRING;
					break;
				case '.':
					if (!quoted) {
						type = Type.DOUBLE;
					}
					lastChar = i;
					break;
				case 't':
				case 'f':
					if (!quoted) {
						type = Type.BOOLEAN;
					}
					break;
				case 'L':
					if (!quoted) {
						type = Type.LONG;
					}
					break;
				case ':':
					if (quoted) {
						break;
					}
					key = json.substring(cursor + 1, lastChar);
					type = Type.INT;
					cursor = i + 1;
					lastChar = -1;
					break;
				case ']':
				case '}':
					if (quoted) {
						break;
					}
					end = true;
				case ',':
					if (quoted) {
						break;
					}
					if (lastChar != -1) {
						Object value = null;
						switch (type) {
							case STRING:
								value= substring(chars, cursor + 1, lastChar);
								break;
							case INT:
								value = Integer.parseInt(json.substring(cursor, lastChar + 1));
								break;
							case LONG:
								value = Long.parseLong(json.substring(cursor, lastChar + 1));
								break;
							case DOUBLE:
								value = Double.parseDouble(json.substring(cursor, lastChar + 1));
								break;
							case BOOLEAN:
								value = chars[cursor] == 't';
						}
						currentParent.add(key, value);
						key = null;
					} else {
						switch (chars[i]) {
							case ']':
							case '}':
								end = true;
								break;
							default:
								end = false;
						}
					}
					type = Type.INT;
					if (end) {
						JSONStorage prev = currentParent;
						currentParent = currentParent.getParent();
						parentType = currentParent instanceof JSONList ? Type.LIST : Type.MAP;
						if (currentParent != null) {
							if (currentParent.getTempKey() != null) {
								currentParent.add(currentParent.getTempKey(), prev);
								currentParent.setTempKey(null);
							}
						}
					}
					lastChar = -1;
					cursor = i + 1;
					end = false;
					break;
				case '{':
				case '[':
					if (quoted) {
						break;
					}
					currentParent.setTempKey(key == null ? "" : key);
					key = null;
					JSONStorage next;
					next = chars[i] == '[' ? new JSONList() : new JSONMap();
					next.setParent(currentParent);
					currentParent = next;
					cursor = i + 1;
					lastChar = -1;
					break;
				default:
					lastChar = i;
			}
		}
		return root;
	}
	
	private static String substring(char[] chars, int start, int end) {
		StringBuilder builder = new StringBuilder();
		for (int i = start; i < end; i++) {
			char c = chars[i];
			if (c == '\\') {
				builder.append(chars[i + 1]);
				i++;
				continue;
			}
			builder.append(c);
		}
		return builder.toString();
	}
	
	private enum Type {
		
		LIST,
		MAP,
		STRING,
		BOOLEAN,
		DOUBLE,
		INT,
		LONG;
		
	}
	
}

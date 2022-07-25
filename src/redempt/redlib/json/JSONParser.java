package redempt.redlib.json;

public class JSONParser {
	
	private static boolean[] whitespace;
	
	static {
		whitespace = new boolean[256];
		whitespace[' '] = true;
		whitespace['\n'] = true;
		whitespace['\t'] = true;
	}
	
	public static JSONMap parseMap(String json) {
		return new JSONParser(json).map();
	}
	
	public static JSONList parseList(String json) {
		return new JSONParser(json).list();
	}
	
	private int pos;
	private String str;
	private StringBuilder builder = new StringBuilder();
	
	private JSONParser(String str) {
		this.str = str;
	}
	
	private char peek() {
		return str.charAt(pos);
	}
	
	private char advance() {
		return str.charAt(pos++);
	}
	
	private void assertChar(char c) {
		if (advance() != c) {
			throw new IllegalArgumentException("Invalid JSON, expected '" + c + "' at position " + (pos - 1));
		}
	}
	
	private boolean isWhitespace() {
		char c = peek();
		return c < 256 && whitespace[c];
	}
	
	private boolean isDigit() {
		return Character.isDigit(peek());
	}
	
	private void whitespace() {
		while (isWhitespace()) {
			pos++;
		}
	}
	
	private long integer() {
		boolean negative = peek() == '-';
		if (negative) {
			pos++;
		}
		int out = 0;
		while (isDigit()) {
			out *= 10;
			out += advance() - '0';
		}
		return negative ? -out : out;
	}
	
	private double decimal(long first) {
		assertChar('.');
		long second = integer();
		double decimal = second * Math.pow(0.1, Math.ceil(Math.log10(second)));
		return decimal + first;
	}
	
	private char escapeSequence() {
		switch (advance()) {
			case 'n':
				return '\n';
			case 't':
				return '\t';
			case 'r':
				return '\r';
			case 'u':
				return (char) Integer.parseInt(str.substring(pos, pos + 4), 16);
			case '"':
				return '"';
			case '\\':
				return '\\';
			default:
				throw new IllegalArgumentException("Invalid escape sequence at position " + pos);
		}
	}
	
	private String string() {
		assertChar('"');
		builder.setLength(0);
		char c;
		while ((c = advance()) != '"') {
			builder.append(c == '\\' ? escapeSequence() : c);
		}
		return builder.toString();
	}
	
	private Object object() {
		switch (peek()) {
			case 't':
				pos += 4;
				return true;
			case 'f':
				pos += 5;
				return false;
			case 'n':
				pos += 4;
				return null;
			case '-':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				long num = integer();
				if (peek() == '.') {
					return decimal(num);
				}
				return num;
			case '.':
				return decimal(0);
			case '"':
				return string();
			case '[':
				return list();
			case '{':
				return map();
			default:
				throw new IllegalArgumentException("Invalid JSON, unknown token at position " + pos);
		}
	}
	
	private JSONList list() {
		assertChar('[');
		whitespace();
		JSONList list = new JSONList();
		while (peek() != ']') {
			list.add(object());
			whitespace();
			if (peek() == ',') {
				pos++;
				whitespace();
			}
		}
		pos++;
		return list;
	}
	
	private JSONMap map() {
		assertChar('{');
		whitespace();
		JSONMap map = new JSONMap();
		while (peek() != '}') {
			String key = string();
			whitespace();
			assertChar(':');
			whitespace();
			map.put(key, object());
			if (peek() == ',') {
				pos++;
				whitespace();
			}
		}
		pos++;
		return map;
	}
	
}


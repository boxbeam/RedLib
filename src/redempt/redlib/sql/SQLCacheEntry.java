package redempt.redlib.sql;

import java.sql.PreparedStatement;
import java.util.Objects;

class SQLCacheEntry {
	
	private Object[] params;
	
	public SQLCacheEntry(Object[] params) {
		this.params = params;
	}
	
	public Object[] getParams() {
		return params;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(params);
	}
	
	@Override
	public boolean equals(Object o) {
		return o != null && o.hashCode() == hashCode();
	}
	
}

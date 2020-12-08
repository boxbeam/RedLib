package redempt.redlib.sql;

import java.util.*;
import java.util.function.Function;

/**
 * A cache to store the values in a single column of a single SQL table
 */
public class SQLCache {
	
	private String tableName;
	private String columnName;
	private String[] primaryKeyNames;
	private String deleteQuery;
	private String selectQuery;
	private String updateQuery;
	private Map<SQLCacheEntry, Object> cache = new HashMap<>();
	private Set<SQLCacheEntry> modified = new HashSet<>();
	private SQLHelper sql;
	
	protected SQLCache(SQLHelper sql, String tableName, String columnName, String... primaryKeyNames) {
		this.tableName = tableName;
		this.columnName = columnName;
		this.primaryKeyNames = primaryKeyNames;
		deleteQuery = "DELETE FROM " + this.tableName + " WHERE " + repeat(primaryKeyNames, " = ?", " AND ");
		selectQuery = "SELECT " + columnName + " FROM " + this.tableName + " WHERE " + repeat(primaryKeyNames, " = ?", " AND ");
		updateQuery = "UPDATE " + this.tableName + " SET " + columnName + " = ? WHERE " + repeat(primaryKeyNames, " = ?", " AND ");
		this.sql = sql;
	}
	
	private String repeat(String[] values, String str, String delimeter) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			builder.append(values[i]).append(str);
			if (i != values.length - 1) {
				builder.append(delimeter);
			}
		}
		return builder.toString();
	}
	
	/**
	 * @return The name of the table this SQLCache is for
	 */
	public String getTableName() {
		return tableName;
	}
	
	/**
	 * @return The name of the column this SQLCache is for
	 */
	public String getColumnName() {
		return columnName;
	}
	
	/**
	 * @return The names of the primary keys used to access and mutate the column this SQLCache is for
	 */
	public String[] getPrimaryKeyNames() {
		return primaryKeyNames;
	}
	
	protected boolean keyNamesMatch(String[] matches) {
		for (String match : matches) {
			if (match.equals(columnName)) {
				return true;
			}
		}
		for (String key : primaryKeyNames) {
			for (String match : matches) {
				if (key.equals(match)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void checkKeys(Object... primaryKeys) {
		if (primaryKeys.length != primaryKeyNames.length) {
			throw new IllegalArgumentException("Expected " + primaryKeyNames.length + " primary keys, got " + primaryKeys.length);
		}
	}
	
	/**
	 * Deletes a row from the table by its primary keys, and removes it from the cache.
	 * This operation will always use a query.
	 * @param primaryKeys The keys to use to delete the row
	 */
	public void delete(Object... primaryKeys) {
		remove(primaryKeys);
		sql.execute(deleteQuery, primaryKeys);
	}
	
	/**
	 * Removes a cached value, but does not affect the table
	 * @param primaryKeys The keys used to access the value
	 */
	public void remove(Object... primaryKeys) {
		checkKeys(primaryKeys);
		SQLCacheEntry entry = new SQLCacheEntry(primaryKeys);
		modified.remove(entry);
		cache.remove(entry);
	}
	
	/**
	 * Updates the cached value for a row
	 * @param value The value to cache
	 * @param primaryKeys The primary keys used to mutate the row
	 */
	public void update(Object value, Object... primaryKeys) {
		checkKeys(primaryKeys);
		SQLCacheEntry entry = new SQLCacheEntry(primaryKeys);
		if (!cache.containsKey(entry)) {
			return;
		}
		cache.remove(entry);
		modified.add(entry);
		cache.put(entry, value);
	}
	
	/**
	 * Gets the cached value for a row, or queries it if it has not been cached yet
	 * @param primaryKeys The primary keys used to access the row
	 * @param <T> The type of the value
	 * @return The value
	 */
	public <T> T select(Object... primaryKeys) {
		return (T) select(o -> sql.querySingleResult(selectQuery, primaryKeys), primaryKeys);
	}
	
	/**
	 * Gets the cached value for a String row, or queries it if it has not been cached yet
	 * @param primaryKeys The primary keys used to access the row
	 * @return The String value
	 */
	public String selectString(Object... primaryKeys) {
		return (String) select(o -> sql.querySingleResultString(selectQuery, primaryKeys), primaryKeys);
	}
	
	/**
	 * Gets the cached value for a Long row, or queries it if it has not been cached yet
	 * @param primaryKeys The primary keys used to access the row
	 * @return The Long value
	 */
	public Long selectLong(Object... primaryKeys) {
		return (Long) select(o -> sql.querySingleResultLong(selectQuery, primaryKeys), primaryKeys);
	}
	
	/**
	 * Checks whether a value has been cached by its primary keys
	 * @param primaryKeys The primary keys used to access the row
	 * @return Whether the value has been cached
	 */
	public boolean isCached(Object... primaryKeys) {
		return cache.containsKey(new SQLCacheEntry(primaryKeys));
	}
	
	private Object select(Function<Object[], ?> supplier, Object... primaryKeys) {
		checkKeys(primaryKeys);
		SQLCacheEntry entry = new SQLCacheEntry(primaryKeys);
		Object value;
		if (!cache.containsKey(entry)) {
			value = supplier.apply(primaryKeys);
			cache.put(entry, value);
		} else {
			 value = cache.get(entry);
		}
		return value;
	}
	
	/**
	 * Clears the cache. WARNING: This will revert all changes that have not been flushed!
	 * No updates performed through {@link SQLCache#update(Object, Object...)} will be committed!
	 */
	public void clear() {
		cache.clear();
	}
	
	/**
	 * Flushes the cache, saving all changes that were made.
	 */
	public void flush() {
		modified.forEach(s -> {
			Object val = cache.get(s);
			Object[] objs = new Object[s.getParams().length + 1];
			objs[0] = val;
			for (int i = 0; i < s.getParams().length; i++) {
				objs[i + 1] = s.getParams()[i];
			}
			sql.execute(updateQuery, objs);
		});
		modified.clear();
	}
	
	/**
	 * Flushes a single value from the cache, saving changes that were made to it
	 * @param primaryKeys The primary keys used to access the row
	 */
	public void flush(Object... primaryKeys) {
		SQLCacheEntry entry = new SQLCacheEntry(primaryKeys);
		Object val = cache.get(entry);
		if (val == null) {
			return;
		}
		Object[] objs = new Object[entry.getParams().length + 1];
		objs[0] = val;
		for (int i = 0; i < entry.getParams().length; i++) {
			objs[i + 1] = entry.getParams()[i];
		}
		sql.execute(updateQuery, objs);
		modified.remove(entry);
	}
	
}

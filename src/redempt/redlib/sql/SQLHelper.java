package redempt.redlib.sql;

import redempt.redlib.RedLib;
import redempt.redlib.misc.Task;

import java.io.Closeable;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Wraps a {@link Connection} and offers helpful methods that don't need to be surrounded in a try/catch
 * @author Redempt
 */
public class SQLHelper implements Closeable {
	
	/**
	 * Opens a SQLite database file
	 * @param file The path to the SQLite database file
	 * @return The Connection to this SQLite database
	 */
	public static Connection openSQLite(java.nio.file.Path file) {
		try {
			Class.forName("org.sqlite.JDBC");
			return DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath().toString());
		} catch (ClassNotFoundException | SQLException e) {
			sneakyThrow(e);
			return null;
		}
	}
	
	/**
	 * Opens a connection to a MySQL database
	 * @param ip The IP address to connect to
	 * @param port The port to connect to
	 * @param username The username to log in with
	 * @param password The password to log in with
	 * @param database The database to use, will be created if it doesn't exist
	 * @return The Connection to the MySQL database
	 */
	public static Connection openMySQL(String ip, int port, String username, String password, String database) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connection = DriverManager.getConnection("jdbc:mysql://" + ip + ":" + port + "/?user=" + username + "&password=" + password);
			connection.createStatement().execute("CREATE DATABASE IF NOT EXISTS " + database + ";");
			connection.createStatement().execute("USE " + database + ";");
			return connection;
		} catch (ClassNotFoundException | SQLException e) {
			sneakyThrow(e);
			return null;
		}
	}
	
	/**
	 * Opens a connection to a MySQL database at localhost:3306
	 * @param username The username to log in with
	 * @param password The password to log in with
	 * @param database The database to use, will be created if it doesn't exist
	 * @return The Connection to the MySQL database
	 */
	public static Connection openMySQL(String username, String password, String database) {
		return openMySQL("localhost", 3306, username, password, database);
	}
	
	private static <T extends Exception> void sneakyThrow(Exception e) throws T {
		throw (T) e;
	}
	
	private Connection connection;
	private List<SQLCache> caches = new ArrayList<>();
	private Task commitTask = null;
	
	/**
	 * Constructs a SQLHelper from a Connection. Get the Connection using one of the static SQLHelper open methods.
	 * @param connection The SQL Connection to wrap
	 */
	public SQLHelper(Connection connection) {
		this.connection = connection;
	}
	
	/**
	 * Creates and adds cache for a certain column
	 * @param tableName The name of the table to create the cache for
	 * @param columnName The name of the column to create the cache for
	 * @param primaryKeyNames The primary keys used to access and mutate the column
	 * @return The cache
	 */
	public SQLCache createCache(String tableName, String columnName, String... primaryKeyNames) {
		SQLCache cache = new SQLCache(this, tableName, columnName, primaryKeyNames);
		caches.add(cache);
		return cache;
	}
	
	/**
	 * Finds matching caches by a pattern and flushes a specific entry from them.
	 * @param pattern The pattern used for {@link SQLHelper#getMatchingCaches(String)}
	 * @param primaryKeys The primary keys used to access the entry
	 */
	public void flushMatchingCaches(String pattern, Object... primaryKeys) {
		getMatchingCaches(pattern).forEach(c -> c.flush(primaryKeys));
	}
	
	/**
	 * Finds matching caches by a pattern and removes a specific entry from them. Useful for saving targeted
	 * cached rows when a column in a certain table is changed
	 * @param pattern The pattern used for {@link SQLHelper#getMatchingCaches(String)}
	 * @param primaryKeys The primary keys used to access the entry
	 */
	public void removeFromMatchingCaches(String pattern, Object... primaryKeys) {
		getMatchingCaches(pattern).forEach(c -> c.remove(primaryKeys));
	}
	
	/**
	 * Finds matching caches by a pattern and flushes, then removes a specific entry from them.
	 * @param pattern The pattern used for {@link SQLHelper#getMatchingCaches(String)}
	 * @param primaryKeys The primary keys used to access the entry
	 */
	public void flushAndRemoveFromMatchingCaches(String pattern, Object... primaryKeys) {
		List<SQLCache> caches = getMatchingCaches(pattern);
		caches.forEach(c -> c.flush(primaryKeys));
		caches.forEach(c -> c.remove(primaryKeys));
	}
	
	/**
	 * Gets the caches matching a pattern
	 * @param pattern The pattern to match. Should be formatted as "tableName.primaryKeyColumnName". Use * to indicate all for either tableName or columnName.
	 *                Use | to indicate or. Primary key column name matches any primary key with the given column name. Useful if you are updating
	 *                a value in a table and want to flush/remove targeted values from the cache.
	 *                Example: *.name|team
	 * @return The list of matching caches
	 */
	public List<SQLCache> getMatchingCaches(String pattern) {
		List<SQLCache> list = new ArrayList<>();
		String[] split = pattern.split("\\.");
		if (split.length != 2) {
			throw new IllegalArgumentException("Pattern to match caches must match tableName.columnName (use * to match all of either)");
		}
		String[] tableName = split[0].split("\\|");
		String[] columnName = split[1].split("\\|");
		for (SQLCache cache : caches) {
			if (!(tableName[0].equals("*") || Arrays.stream(tableName).anyMatch(s -> s.equals(cache.getTableName())))) {
				continue;
			}
			if (!(columnName[0].equals("*") || cache.keyNamesMatch(columnName))) {
				continue;
			}
			list.add(cache);
		}
		list.forEach(c -> {
			System.out.println(c.getTableName() + ": " + c.getColumnName());
		});
		return list;
	}
	
	/**
	 * @return The list of caches for this SQLHelper
	 */
	public List<SQLCache> getCaches() {
		return caches;
	}
	
	/**
	 * Calls {@link SQLCache#flush()} on all caches owned by this SQLHelper
	 */
	public void flushAllCaches() {
		caches.forEach(SQLCache::flush);
	}
	
	/**
	 * Calls {@link SQLCache#clear()} on all caches owned by this SQLHelper
	 */
	public void clearAllCaches() {
		caches.forEach(SQLCache::clear);
	}
	
	/**
	 * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed
	 * @param command The SQL command to execute
	 * @param fields A vararg of the fields to set in the prepared statement
	 */
	public void execute(String command, Object... fields) {
		try {
			PreparedStatement statement = prepareStatement(command, fields);
			statement.execute();
		} catch (SQLException e) {
			sneakyThrow(e);
		}
	}
	
	/**
	 * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed,
	 * returning the value in the first column of the first row in the results
	 * @param query The SQL query to execute
	 * @param fields A vararg of the fields to set in the prepared statement
	 * @param <T> The type to cast the return value to
	 * @return The value in the first column of the first row of the returned results, or null if none is present
	 */
	public <T> T querySingleResult(String query, Object... fields) {
		try {
			PreparedStatement statement = prepareStatement(query, fields);
			ResultSet results = statement.executeQuery();
			if (!results.next()) {
				return null;
			}
			T obj = (T) results.getObject(1);
			results.close();
			return obj;
		} catch (SQLException e) {
			sneakyThrow(e);
			return null;
		}
	}
	
	/**
	 * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed,
	 * returning the value in the first column of the first row in the results as a String.
	 * @param query The SQL query to execute
	 * @param fields A vararg of the fields to set in the prepared statement
	 * @return The String in the first column of the first row of the returned results, or null if none is present
	 * @implNote This method exists because {@link ResultSet#getObject(int)} can return an Integer if the String in the
	 * column can be parsed into one.
	 */
	public String querySingleResultString(String query, Object... fields) {
		try {
			PreparedStatement statement = prepareStatement(query, fields);
			ResultSet results = statement.executeQuery();
			if (!results.next()) {
				return null;
			}
			return results.getString(1);
		} catch (SQLException e) {
			sneakyThrow(e);
			return null;
		}
	}
	
	/**
	 * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed,
	 * returning the value in the first column of the first row in the results as a Long.
	 * @param query The SQL query to execute
	 * @param fields A vararg of the fields to set in the prepared statement
	 * @return The String in the first column of the first row of the returned results, or null if none is present
	 * @implNote This method exists because {@link ResultSet#getObject(int)} can return an Integer if the Long in the
	 * column can be parsed into one.
	 */
	public Long querySingleResultLong(String query, Object... fields) {
		try {
			PreparedStatement statement = prepareStatement(query, fields);
			ResultSet results = statement.executeQuery();
			if (!results.next()) {
				return null;
			}
			return results.getLong(1);
		} catch (SQLException e) {
			sneakyThrow(e);
			return null;
		}
	}
	
	/**
	 * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed,
	 * returning a list of values in the first column of each row in the results
	 * @param query The SQL query to execute
	 * @param fields A vararg of the fields to set in the prepared statement
	 * @param <T> The type to populate the list with and return
	 * @return A list of the value in the first column of each row returned by the query
	 */
	public <T> List<T> queryResultList(String query, Object... fields) {
		List<T> list = new ArrayList<>();
		try {
			PreparedStatement statement = prepareStatement(query, fields);
			ResultSet results = statement.executeQuery();
			while (results.next()) {
				list.add((T) results.getObject(1));
			}
			results.close();
		} catch (SQLException e) {
			sneakyThrow(e);
		}
		return list;
	}
	
	/**
	 * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed,
	 * returning a String list of values in the first column of each row in the results
	 * @param query The SQL query to execute
	 * @param fields A vararg of the fields to set in the prepared statement
	 * @return A String list of the value in the first column of each row returned by the query
	 * @implNote This method exists because {@link ResultSet#getObject(int)} can return an Integer if the String in the
	 * column can be parsed into one.
	 */
	public List<String> queryResultStringList(String query, Object... fields) {
		List<String> list = new ArrayList<>();
		try {
			PreparedStatement statement = prepareStatement(query, fields);
			ResultSet results = statement.executeQuery();
			while (results.next()) {
				list.add(results.getString(1));
			}
			results.close();
		} catch (SQLException e) {
			sneakyThrow(e);
		}
		return list;
	}
	
	/**
	 * Executes a SQL query as a prepared statement, setting its fields to the elements of the vararg passed.
	 * Returns a {@link Results}, which wraps a {@link ResultSet} for easier use
	 * @param query The SQL query to execute
	 * @param fields A vararg of the fields to set in the prepared statement
	 * @return The results of the query
	 */
	public Results queryResults(String query, Object... fields) {
		try {
			ResultSet results = prepareStatement(query, fields).executeQuery();
			return new Results(results);
		} catch (SQLException e) {
			sneakyThrow(e);
			return null;
		}
	}
	
	/**
	 * @return The Connection this SQLHelper wraps
	 */
	public Connection getConnection() {
		return connection;
	}
	
	/**
	 * Sets the wrapped connection's auto-commit property. Calling this method will automatically disable
	 * the task started by {@link SQLHelper#setCommitInterval(int)}.
	 * @param autoCommit The auto-commit property - whether it will commit with every command
	 */
	public void setAutoCommit(boolean autoCommit) {
		try {
			setCommitInterval(-1);
			connection.setAutoCommit(autoCommit);
		} catch (SQLException e) {
			sneakyThrow(e);

		}
	}
	
	/**
	 * @return The auto-commit property of the wrapped connection
	 */
	public boolean isAutoCommit() {
		try {
			return connection.getAutoCommit();
		} catch (SQLException e) {
			sneakyThrow(e);
			return false;
		}
	}
	
	/**
	 * Starts a task to call commit() on this SQLHelper every n ticks. Pass -1 to disable.
	 * Automatically sets autoCommit to false.
	 * @param ticks The number of ticks between commits, or -1 to disable
	 */
	public void setCommitInterval(int ticks) {
		if (commitTask != null) {
			commitTask.cancel();
			commitTask = null;
		}
		if (ticks == -1) {
			return;
		}
		setAutoCommit(false);
		commitTask = Task.syncRepeating(RedLib.getInstance(), this::commit, ticks, ticks);
	}
	
	/**
	 * Flushes all caches and commits the transaction
	 */
	public void commit() {
		try {
			flushAllCaches();
			connection.commit();
		} catch (SQLException e) {
			sneakyThrow(e);
		}
	}
	
	/**
	 * Prepares a statement, setting its fields to the elements of the vararg passed
	 * @param query The SQL query to prepare
	 * @param fields A vararg of the fields to set in the prepared statement
	 * @return The PreparedStatement with its fields set
	 */
	public PreparedStatement prepareStatement(String query, Object... fields) {
		try {
			PreparedStatement statement = connection.prepareStatement(query);
			int i = 1;
			for (Object object : fields) {
				statement.setObject(i, object);
				i++;
			}
			return statement;
		} catch (SQLException e) {
			sneakyThrow(e);
			return null;
		}
	}
	
	/**
	 * Closes the underlying connection this SQLHelper wraps
	 */
	@Override
	public void close() {
		try {
			setCommitInterval(-1);
			connection.close();
		} catch (SQLException e) {
			sneakyThrow(e);
		}
	}
	
	/**
	 * Wraps a {@link ResultSet} with easier use
	 * @author Redempt
	 */
	public static class Results implements Closeable {
		
		private ResultSet results;
		private boolean empty;
		
		private Results(ResultSet results) {
			this.results = results;
			try {
				empty = !results.next();
			} catch (SQLException e) {
				sneakyThrow(e);
			}
		}
		
		/**
		 * @return False if the first call of {@link ResultSet#next()} on the wrapped ResultSet returned false,
		 * true otherwise
		 */
		public boolean isEmpty() {
			return empty;
		}
		
		/**
		 * Moves to the next row in the wrapped ResultSet. Note that this method is called immediately when the
		 * Results object is constructed, and does not need to be called to retrieve the items in the first row.
		 * @return True if there is another row available in the wrapped ResultSet
		 */
		public boolean next() {
			try {
				return results.next();
			} catch (SQLException e) {
				sneakyThrow(e);
				return false;
			}
		}
		
		/**
		 * Performs an operation on every row in these Results, passing itself each time it iterates to a new row
		 * @param lambda The callback to be run on every row in these Results
		 */
		public void forEach(Consumer<Results> lambda) {
			if (isEmpty()) {
				return;
			}
			lambda.accept(this);
			while (next()) {
				lambda.accept(this);
			}
			close();
		}
		
		/**
		 * Gets an Object in the given column in the current row
		 * @param column The index of the column to get, starting at 1
		 * @param <T> The type to cast the return value to
		 * @return The value in the column
		 */
		public <T> T get(int column) {
			try {
				return (T) results.getObject(column);
			} catch (SQLException e) {
				sneakyThrow(e);
				return null;
			}
		}
		
		/**
		 * Gets a String in the given column in the current row
		 * @param column The index of the column to get, starting at 1
		 * @return The String in the column
		 * @implNote This method exists because {@link ResultSet#getObject(int)} can return an Integer if the String in the
		 * column can be parsed into one.
		 */
		public String getString(int column) {
			try {
				return results.getString(column);
			} catch (SQLException e) {
				sneakyThrow(e);
				return null;
			}
		}
		
		/**
		 * Gets a Long in the given column in the current row
		 * @param column The index of the column to get, starting at 1
		 * @return The String in the column
		 * @implNote This method exists because {@link ResultSet#getObject(int)} can return an Integer if the Long in the
		 * column can be parsed into one.
		 */
		public Long getLong(int column) {
			try {
				return results.getLong(column);
			} catch (SQLException e) {
				sneakyThrow(e);
				return null;
			}
		}
		
		/**
		 * Closes the wrapped ResultSet. Call this when you are done using these Results.
		 */
		@Override
		public void close() {
			try {
				if (results.isClosed()) {
					return;
				}
				results.close();
			} catch (SQLException e) {
				sneakyThrow(e);
			}
		}
		
	}
	
}

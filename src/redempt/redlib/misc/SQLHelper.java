package redempt.redlib.misc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLHelper {
	
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
			e.printStackTrace();
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
			e.printStackTrace();
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

}

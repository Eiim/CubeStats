package page.eiim.cubestats.web;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnector {

	public static Connection getConnection(String user, String password, String url) throws SQLException {
		Connection conn = null;
		Properties connectionProps = new Properties();
		connectionProps.put("user", user);
		connectionProps.put("password", password);
		conn = DriverManager.getConnection(url, connectionProps);

		System.out.println("Connected to database");
		return conn;
	}
}

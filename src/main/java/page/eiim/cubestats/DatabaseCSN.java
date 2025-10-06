package page.eiim.cubestats;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/*
 * This is suboptimal but okay for now. In the future, should migrate to using a connection pool.
 * See https://jdbc.postgresql.org/documentation/datasource/, https://commons.apache.org/proper/commons-dbcp/
 */

public class DatabaseCSN {
	
	public static Connection getConnection(TaskSettings settings, DefaultSchema schema) throws SQLException {
		Connection conn = null;
		Properties connectionProps = new Properties();
		connectionProps.put("user", settings.dbUserName);
		connectionProps.put("password", settings.dbPassword);
		//connectionProps.put("connectionCollation", "utf8mb4_unicode_ci");
		String dbUrl = switch(schema) {
			case STAGING -> settings.dbUrlSatging;
			case LIVE -> settings.dbUrlLive;
		};

		conn = DriverManager.getConnection(dbUrl, connectionProps);

		//System.out.println("Connected to database");
		return conn;
	}
	
	public static enum DefaultSchema { STAGING, LIVE };
}

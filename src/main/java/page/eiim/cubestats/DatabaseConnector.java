package page.eiim.cubestats;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mariadb.jdbc.MariaDbPoolDataSource;

public class DatabaseConnector {
	
	private static MariaDbPoolDataSource dataSourceA;
	private static MariaDbPoolDataSource dataSourceB;
	private static DatabaseSchema liveSchema;
	
	public static void initialize(String user, String password, String urlA, String urlB, String schemaA, String schemaB) throws SQLException {
		dataSourceA = new MariaDbPoolDataSource();
		dataSourceA.setUser(user);
		dataSourceA.setPassword(password);
		dataSourceA.setUrl(urlA);
		
		dataSourceB = new MariaDbPoolDataSource();
		dataSourceB.setUser(user);
		dataSourceB.setPassword(password);
		dataSourceB.setUrl(urlB);
		
		Connection connA = dataSourceA.getConnection();
		Connection connB = dataSourceB.getConnection();
		ResultSet rsA = connA.prepareStatement("SELECT cs_value FROM cs_metadata WHERE cs_key = \"status\"").executeQuery();
		ResultSet rsB = connB.prepareStatement("SELECT cs_value FROM cs_metadata WHERE cs_key = \"status\"").executeQuery();
		
		if(rsA.next() && rsB.next()) {
			String statusA = rsA.getString(1);
			String statusB = rsB.getString(1);
			if(statusA.equals("live") && statusB.equals("empty")) {
				liveSchema = DatabaseSchema.A;
			} else if(statusA.equals("empty") && statusB.equals("live")) {
				liveSchema = DatabaseSchema.B;
			} else if(statusA.equals("live") && statusB.equals("staging")){
				System.out.println("Warning: database B is already set to staging. Wiping B and using A as live.");
				connA.prepareStatement("DROP SCHEMA IF EXISTS " + schemaA + "").executeUpdate();
				connA.prepareStatement("CREATE SCHEMA " + schemaA).executeUpdate();
				connA.prepareStatement("CREATE TABLE " + schemaA + ".cs_metadata (cs_key VARCHAR(255) PRIMARY KEY, cs_value VARCHAR(255))").executeUpdate();
				connA.prepareStatement("INSERT INTO " + schemaA + ".cs_metadata (cs_key, cs_value) VALUES ('status', 'empty')").executeUpdate();
				liveSchema = DatabaseSchema.A;
			} else if(statusA.equals("staging") && statusB.equals("live")){
				System.out.println("Warning: database A is already set to staging. Wiping A and using B as live.");
				connB.prepareStatement("DROP SCHEMA IF EXISTS " + schemaB + "").executeUpdate();
				connB.prepareStatement("CREATE SCHEMA " + schemaB).executeUpdate();
				connB.prepareStatement("CREATE TABLE " + schemaB + ".cs_metadata (cs_key VARCHAR(255) PRIMARY KEY, cs_value VARCHAR(255))").executeUpdate();
				connB.prepareStatement("INSERT INTO " + schemaB + ".cs_metadata (cs_key, cs_value) VALUES ('status', 'empty')").executeUpdate();
				liveSchema = DatabaseSchema.B;
			} else {
				System.err.println("Could not autodetect live/staging databases, invalid status values: A='" + statusA + "', B='" + statusB + "'");
			}
		} else {
			System.err.println("Could not autodetect live/staging databases, missing metadata 'status' key");
		}
		
		System.out.println("Initialized database connection pool");
	}

	public static Connection getLiveConnection() throws SQLException {
		return liveSchema == DatabaseSchema.A ? dataSourceA.getConnection() : dataSourceB.getConnection();
	}
	
	public static Connection getStagingConnection() throws SQLException {
		return liveSchema == DatabaseSchema.A ? dataSourceB.getConnection() : dataSourceA.getConnection();
	}
	
	public static void swapDatabases() {
		liveSchema = (liveSchema == DatabaseSchema.A) ? DatabaseSchema.B : DatabaseSchema.A;
	}
	
	public static DatabaseSchema getLiveSchema() {
		return liveSchema;
	}
	
	public static enum DatabaseSchema {
		A, B;
	}
}

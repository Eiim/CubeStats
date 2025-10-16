package page.eiim.cubestats.tasks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import page.eiim.cubestats.DatabaseCSN;
import page.eiim.cubestats.Settings;

public class TaskMigrateDatabase extends Task {
	
	private final String staging;
	private final String live;
	
	public TaskMigrateDatabase(Settings settings) {
		super(settings);
		synchronized(settings) {
			staging = settings.dbSchemaStaging;
			live = settings.dbSchemaLive;
		}
	}
	
	@Override
	public String name() {
		return "Migrate Database";
	}

	@Override
	public void run() {
		try {
			Connection conn = DatabaseCSN.getConnection(settings, DatabaseCSN.DefaultSchema.STAGING);
			
			conn.prepareStatement("DROP SCHEMA IF EXISTS " + live + "").executeUpdate();
			conn.prepareStatement("CREATE SCHEMA " + live).executeUpdate();
			
			ResultSet rs = conn.prepareStatement("SELECT table_name FROM information_schema.tables WHERE table_schema = '" + staging + "'").executeQuery();
			while(rs.next()) {
				String tableName = rs.getString(1);
				System.out.println("Migrating table " + tableName);
				conn.prepareStatement("CREATE TABLE " + live + "." + tableName + " LIKE " + staging + "." + tableName).executeUpdate();
				conn.prepareStatement("INSERT INTO " + live + "." + tableName + " SELECT * FROM " + staging + "." + tableName).executeUpdate();
			}
			
			conn.prepareStatement("DROP SCHEMA " + staging).executeUpdate();
			conn.prepareStatement("CREATE SCHEMA " + staging).executeUpdate();
			
			result = new TaskResult(true, "Finished migrating database tables");
			isDone = true;
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			result = new TaskResult(false, e.getMessage());
			isDone = true;
			return;
		}
	}
}

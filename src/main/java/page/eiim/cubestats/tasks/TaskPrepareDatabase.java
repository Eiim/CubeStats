package page.eiim.cubestats.tasks;

import java.sql.Connection;
import java.sql.SQLException;

import page.eiim.cubestats.DatabaseConnector;
import page.eiim.cubestats.Settings;

public class TaskPrepareDatabase extends Task {
	
	public TaskPrepareDatabase(Settings settings) {
		super(settings);
	}
	
	@Override
	public String name() {
		return "Prepare database for import";
	}

	@Override
	public void run() {
		try {
			Connection conn = DatabaseConnector.getStagingConnection();
			String stagingSchema = (DatabaseConnector.getLiveSchema() == DatabaseConnector.DatabaseSchema.A ? settings.dbSchemaB : settings.dbSchemaA);
			
			// Should be blank, but just in case, drop and recreate
			conn.prepareStatement("DROP SCHEMA IF EXISTS " + stagingSchema + "").executeUpdate();
			conn.prepareStatement("CREATE SCHEMA " + stagingSchema).executeUpdate();
			conn.prepareStatement("CREATE TABLE " + stagingSchema + ".cs_metadata (cs_key VARCHAR(255) PRIMARY KEY, cs_value VARCHAR(255))").executeUpdate();
			conn.prepareStatement("INSERT INTO " + stagingSchema + ".cs_metadata (cs_key, cs_value) VALUES ('status', 'staging')").executeUpdate();
			
			result = new TaskResult(true, "Finished cleaning up old database tables");
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

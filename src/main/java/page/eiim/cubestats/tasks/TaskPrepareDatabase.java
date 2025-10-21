package page.eiim.cubestats.tasks;

import java.sql.Connection;
import java.sql.SQLException;

import page.eiim.cubestats.DatabaseCSN;
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
			Connection conn = DatabaseCSN.getConnection(settings, stagingSchema);
			
			// Should be blank, but just in case, drop and recreate
			conn.prepareStatement("DROP SCHEMA IF EXISTS " + stagingSchema.name() + "").executeUpdate();
			conn.prepareStatement("CREATE SCHEMA " + stagingSchema.name()).executeUpdate();
			conn.prepareStatement("CREATE TABLE " + stagingSchema.name() + ".cs_metadata (cs_key VARCHAR(255) PRIMARY KEY, cs_value VARCHAR(255))").executeUpdate();
			conn.prepareStatement("INSERT INTO " + stagingSchema.name() + ".cs_metadata (cs_key, cs_value) VALUES ('status', 'staging')").executeUpdate();
			
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

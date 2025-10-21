package page.eiim.cubestats.tasks;

import java.sql.Connection;
import java.sql.SQLException;

import page.eiim.cubestats.DatabaseCSN;
import page.eiim.cubestats.Settings;

public class TaskMigrateDatabase extends Task {
	
	public TaskMigrateDatabase(Settings settings) {
		super(settings);
	}
	
	@Override
	public String name() {
		return "Migrate Database";
	}

	@Override
	public void run() {
		try {
			Connection conn = DatabaseCSN.getConnection(settings, stagingSchema);
			
			conn.prepareStatement("DROP SCHEMA IF EXISTS " + liveSchema.name() + "").executeUpdate();
			conn.prepareStatement("CREATE SCHEMA " + liveSchema.name()).executeUpdate();
			conn.prepareStatement("CREATE TABLE " + liveSchema.name() + ".cs_metadata (cs_key VARCHAR(255) PRIMARY KEY, cs_value VARCHAR(255))").executeUpdate();
			conn.prepareStatement("INSERT INTO " + liveSchema.name() + ".cs_metadata (cs_key, cs_value) VALUES ('status', 'empty')").executeUpdate();
			
			conn.prepareStatement("UPDATE " + stagingSchema.name() + ".cs_metadata SET cs_value = \"live\" WHERE cs_key = \"status\"").executeUpdate();
			
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

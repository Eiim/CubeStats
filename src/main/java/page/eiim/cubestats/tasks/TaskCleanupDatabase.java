package page.eiim.cubestats.tasks;

import java.sql.Connection;
import java.sql.SQLException;

import page.eiim.cubestats.DatabaseCSN;
import page.eiim.cubestats.TaskSettings;

public class TaskCleanupDatabase extends Task {
	
	private final String staging;
	
	public TaskCleanupDatabase(TaskSettings settings) {
		super(settings);
		synchronized(settings) {
			staging = settings.dbSchemaStaging;
		}
	}
	
	@Override
	public String name() {
		return "Cleanup Database";
	}

	@Override
	public void run() {
		try {
			Connection conn = DatabaseCSN.getConnection(settings, DatabaseCSN.DefaultSchema.STAGING);
			
			conn.prepareStatement("DROP SCHEMA IF EXISTS " + staging + "").executeUpdate();
			conn.prepareStatement("CREATE SCHEMA " + staging).executeUpdate();
			//conn.prepareStatement("GRANT ALL ON SCHEMA " + staging + " TO " + dbOwner).executeUpdate();
			
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

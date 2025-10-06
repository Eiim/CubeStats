package page.eiim.cubestats.tasks;

import java.sql.Connection;
import java.sql.SQLException;

import page.eiim.cubestats.DatabaseCSN;
import page.eiim.cubestats.TaskSettings;

public class TaskCSEvents extends Task {
	
	public TaskCSEvents(TaskSettings settings) {
		super(settings);
	}
	
	@Override
	public String name() {
		return "Generate Custom Events Table";
	}

	@Override
	public void run() {
		try {
			Connection conn = DatabaseCSN.getConnection(settings, DatabaseCSN.DefaultSchema.STAGING);
			
			conn.prepareStatement("DROP TABLE IF EXISTS cs_events").executeUpdate();
			conn.prepareStatement("CREATE TABLE cs_events AS (SELECT *, TRUE AS is_wca, RANK < 900 AS is_active FROM EVENTS)").executeUpdate();
			
			result = new TaskResult(true, "Finished making custom events table");
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

package page.eiim.cubestats.tasks;

import java.io.File;

import page.eiim.cubestats.TaskSettings;

public class TaskImportWCADatabase extends Task {

	private final File dataDirectory;
	private final String mysqlExe;
	private final String dbHost;
	private final int dbPort;
	private final String dbUserName;
	private final String dbPassword;
	private final String dbSchemaStaging;
	
	public TaskImportWCADatabase(TaskSettings settings) {
		synchronized (settings) {
			dataDirectory = settings.dataDirectory;
			mysqlExe = settings.mySQLExe;
			dbHost = settings.dbHost;
			dbPort = settings.dbPort;
			dbUserName = settings.dbUserName;
			dbPassword = settings.dbPassword;
			dbSchemaStaging = settings.dbSchemaStaging;
		}
	}

	@Override
	public String name() {
		return "Import WCA Database Dump";
	}

	@Override
	public void run() {
		
		// Execute mysql command line tool to import the database dump
		ProcessBuilder pb = new ProcessBuilder(
				mysqlExe,
				"-h", dbHost,
				"-P", Integer.toString(dbPort),
				"-u", dbUserName,
				"-p" + dbPassword,
				dbSchemaStaging
		);
		pb.directory(dataDirectory);
		pb.redirectInput(new File(dataDirectory, "wca-developer-database-dump.sql"));
		try {
			Process process = pb.start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				result = new TaskResult(false, "Importing database dump failed with exit code " + exitCode);
				isDone = true;
				return;
			}
		} catch (Exception e) {
			result = new TaskResult(false, "Importing database dump failed: " + e.getMessage());
			isDone = true;
			return;
		}
		
		result = new TaskResult(true, "Database dump successfully imported.");
		isDone = true;
	}
}

package page.eiim.cubestats.tasks;

import java.io.File;

import page.eiim.cubestats.Settings;

public class TaskImportWCADatabase extends Task {
	
	public TaskImportWCADatabase(Settings settings) {
		super(settings);
	}

	@Override
	public String name() {
		return "Import WCA Database Dump";
	}

	@Override
	public void run() {
		
		// Execute mysql command line tool to import the database dump
		ProcessBuilder pb = new ProcessBuilder(
				settings.mySQLExe,
				"-h", settings.dbHost,
				"-P", Integer.toString(settings.dbPort),
				"-u", settings.dbUserName,
				"-p" + settings.dbPassword,
				stagingSchema.name()
		);
		pb.directory(settings.dataDirectory);
		pb.redirectInput(new File(settings.dataDirectory, "wca-developer-database-dump.sql"));
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

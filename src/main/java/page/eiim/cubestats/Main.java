package page.eiim.cubestats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import page.eiim.cubestats.web.MainServer;

public class Main {
	
	public static void main(String[] args) {
		String configPath;
		if(args.length > 0 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
			printHelp();
			return;
		}
		boolean hasConfigArg = args.length > 0 && !args[0].startsWith("--");
		if(hasConfigArg) {
			configPath = args[0];
		} else {
			configPath = "config.json";
		}
		
		HashMap<String, String> configArgs = new HashMap<>();
		for(int i = hasConfigArg ? 1 : 0; i+1 < args.length; i += 2) {
			String arg = args[i];
			if(arg.startsWith("--")) {
				configArgs.put(arg.substring(2), args[i+1]);
			} else {
				throw new IllegalArgumentException("Expected argument starting with --, got: " + arg);
			}
		}
		
		System.out.println("Loading configuration from " + configPath);
		Settings settings = new Settings();
		JsonObject root;
		JsonObject networking = null;
		try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
			root = JsonParser.parseReader(reader).getAsJsonObject();
			for(var entry : root.entrySet()) {
				String key = entry.getKey();
				if("help".equals(key)) {
					printHelp();
					return;
				}
				JsonElement value;
				if(configArgs.containsKey(key)) {
					value = JsonParser.parseString(configArgs.get(key));
				} else {
					value = entry.getValue();
				}
				
				switch(key) {
					case "networking" -> networking = value.getAsJsonObject();
					case "export_metadata_url" -> settings.exportMetadataUrl = value.getAsString();
					case "database_dump_url" -> settings.databaseDumpUrl = value.getAsString();
					case "user_agent" -> settings.userAgent = value.getAsString();
					case "data_directory" -> settings.dataDirectory = new File(value.getAsString());
					case "last_dump_metadata_file" -> settings.lastDumpMetadataFile = new File(value.getAsString());
					
					case "my_sql_exe" -> settings.mySQLExe = value.getAsString();
					case "db_user_name" -> settings.dbUserName = value.getAsString();
					case "db_password" -> settings.dbPassword = value.getAsString();
					case "db_host" -> settings.dbHost = value.getAsString();
					case "db_port" -> settings.dbPort = value.getAsInt();
					case "db_schema_staging" -> settings.dbSchemaStaging = value.getAsString();
					case "db_schema_live" -> settings.dbSchemaLive = value.getAsString();
					case "db_url_staging" -> settings.dbUrlSatging = value.getAsString();
					case "db_url_live" -> settings.dbUrlLive = value.getAsString();
					case "db_schema_owner" -> settings.dbSchemaOwner = value.getAsString();
					
					case "import_charset" -> settings.importCharset = Charset.forName(value.getAsString());
					
					case "min_thread_pool_size" -> settings.minThreadPoolSize = value.getAsInt();
					case "max_thread_pool_size" -> settings.maxThreadPoolSize = value.getAsInt();
					case "resources_root" -> settings.resourcesRoot = new File(value.getAsString());
					case "hostname" -> settings.hostname = value.getAsString();
					case "enable_request_logging" -> settings.enableRequestLogging = value.getAsBoolean();
					
					case "no_import" -> settings.noImport = value.getAsBoolean();
					case "no_webserver" -> settings.noWebserver = value.getAsBoolean();
					
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		if(!settings.noImport) {
			DAGScheduler scheduler = new DAGScheduler(settings);
			scheduler.runAllTasks();
		}
		if(!settings.noWebserver) {
			if(networking == null) {
				System.err.println("No networking configuration found, can't run web server");
			} else {
				MainServer.run(settings, networking);
			}
		}
	}
	
	// TODO: check if tabs are correct
	public static void printHelp() {
		System.out.println("CubeStats, v0");
		System.out.println("Usage: java -jar CubeStats.jar [path/to/config.json] ...");
		System.out.println("Further options can be passed as --key value pairs, overriding the config file.");
		System.out.println("Available options:");
		System.out.println("--help\t\t\t\tPrint this help message (not available as a config file option)");
		
		System.out.println();
		System.out.println("MariaDB database options:");
		System.out.println("--my_sql_exe\t\t\tPath to the MariaDB executable");
		System.out.println("--db_user_name\t\t\tDatabase user name");
		System.out.println("--db_password\t\t\tDatabase user password");
		System.out.println("--db_host\t\t\tDatabase host");
		System.out.println("--db_port\t\t\tDatabase port");
		System.out.println("--db_schema_staging\t\tDatabase schema for staging data");
		System.out.println("--db_schema_live\t\tDatabase schema for live data");
		System.out.println("--db_schema_owner\t\tDatabase schema owner (for granting permissions)");
		
		System.out.println();
		System.out.println("Web server options:");
		System.out.println("--min_thread_pool_size\t\tMinimum thread pool size for the web server");
		System.out.println("--max_thread_pool_size\t\tMaximum thread pool size for the web server");
		System.out.println("--resources_root\t\tPath to the web server resources. Should be distributed with the jar");
		System.out.println("--hostname\t\t\tHostname to bind the web server to");
		System.out.println("--enable_request_logging\tWhether to enable request logging in the web server");
		
		System.out.println();
		System.out.println("Other options:");
		System.out.println("--data_directory\t\tDirectory to store raw WCA database export");
		
		System.out.println();
		System.out.println("Advanced options (only change if there's a problem and you know what you're doing!):");
		System.out.println("--export_metadata_url\t\tWCA export API URL");
		System.out.println("--database_dump_url\t\tURL for the WCA database dump. If unset, found from the metadata file.");
		System.out.println("--user_agent\t\t\tUser-Agent header to use for HTTP requests");
		System.out.println("--last_dump_metadata_file\tMetadata file for the last downloaded database dump. Defaults to lastDumpMetadata.txt in the data directory");
		System.out.println("--db_url_staging\t\tJDBC URL for the staging database");
		System.out.println("--db_url_live\t\t\tJDBC URL for the live database");
		System.out.println("--import_charset\t\tCharacter set of the WCA export files");
	}

}

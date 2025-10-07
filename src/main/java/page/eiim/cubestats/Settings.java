package page.eiim.cubestats;

import java.io.File;
import java.nio.charset.Charset;

public final class Settings {

	public boolean noImport = false;
	public boolean noWebserver = false;
	
	public String exportMetadataUrl = "https://www.worldcubeassociation.org/api/v0/export/public";
	public String databaseDumpUrl = "https://assets.worldcubeassociation.org/export/developer/wca-developer-database-dump.zip";
	public String userAgent = "CubeStatsBot/0.1";
	public File dataDirectory = null; // Must be set
	public File lastDumpMetadataFile = new File(dataDirectory, "lastDumpMetadata.txt");
	
	public String mySQLExe = "mysql";
	
	public String dbUserName = "cubestats";
	public String dbPassword = "cubing";
	public String dbHost = "localhost";
	public int dbPort = 3306;
	public String dbSchemaStaging = "staging";
	public String dbSchemaLive = "live";
	public String dbUrlSatging = "jdbc:mariadb://" + dbHost + ":" + dbPort + "/" + dbSchemaStaging;
	public String dbUrlLive = "jdbc:mariadb://" + dbHost + ":" + dbPort + "/" + dbSchemaLive;
	public String dbSchemaOwner = "root";
	
	public Charset importCharset = Charset.forName("UTF-8");
	
	public int minThreadPoolSize = 4;
	public int maxThreadPoolSize = 8;
	public File resourcesRoot = null; // Must be set
	public String hostname = "localhost";
	public boolean enableRequestLogging = true;
	
	public Settings() {
		// TODO Auto-generated constructor stub
	}

}

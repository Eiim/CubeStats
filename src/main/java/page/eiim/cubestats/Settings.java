package page.eiim.cubestats;

import java.io.File;
import java.nio.charset.Charset;

public final class Settings {

	public final boolean noImport;
	public final boolean noWebserver;
	
	public final String exportMetadataUrl;
	public final String databaseDumpUrl;
	public final String userAgent;
	public final File dataDirectory;
	public final File lastDumpMetadataFile;
	
	public final String mySQLExe;
	
	public final String dbUserName;
	public final String dbPassword;
	public final String dbHost;
	public final int dbPort;
	public final String dbSchemaStaging;
	public final String dbSchemaLive;
	public final String dbUrlStaging;
	public final String dbUrlLive;
	
	public final Charset importCharset;
	
	public final int minThreadPoolSize;
	public final int maxThreadPoolSize;
	public final File resourcesRoot;
	public final String hostname;
	public final boolean enableRequestLogging;
	
	private Settings(Builder b) {
		noImport = b.noImport;
		noWebserver = b.noWebserver;
		exportMetadataUrl = b.exportMetadataUrl;
		databaseDumpUrl = b.databaseDumpUrl;
		userAgent = b.userAgent;
		dataDirectory = b.dataDirectory;
		mySQLExe = b.mySQLExe;
		dbUserName = b.dbUserName;
		dbPassword = b.dbPassword;
		dbHost = b.dbHost;
		dbPort = b.dbPort;
		dbSchemaStaging = b.dbSchemaStaging;
		dbSchemaLive = b.dbSchemaLive;
		importCharset = b.importCharset;
		minThreadPoolSize = b.minThreadPoolSize;
		maxThreadPoolSize = b.maxThreadPoolSize;
		resourcesRoot = b.resourcesRoot;
		hostname = b.hostname;
		enableRequestLogging = b.enableRequestLogging;
		
		if(b.dbUrlStaging == null) {
			dbUrlStaging = "jdbc:mariadb://" + dbHost + ":" + dbPort + "/" + dbSchemaStaging;
		} else {
			dbUrlStaging = b.dbUrlStaging;
		}
		
		if(b.dbUrlLive == null) {
			dbUrlLive = "jdbc:mariadb://" + dbHost + ":" + dbPort + "/" + dbSchemaLive;
		} else {
			dbUrlLive = b.dbUrlLive;
		}
		
		if(b.lastDumpMetadataFile == null) {
			lastDumpMetadataFile = new File(dataDirectory, "lastDumpMetadata.txt");
		} else {
			lastDumpMetadataFile = b.lastDumpMetadataFile;
		}
	}
	
	public static class Builder {
		public boolean noImport = false;
		public boolean noWebserver = false;
		
		public String exportMetadataUrl = "https://www.worldcubeassociation.org/api/v0/export/public";
		public String databaseDumpUrl = "https://assets.worldcubeassociation.org/export/developer/wca-developer-database-dump.zip";
		public String userAgent = "CubeStatsBot/0.1";
		public File dataDirectory = null; // Must be set
		public File lastDumpMetadataFile = null;
		
		public String mySQLExe = "mysql";
		
		public String dbUserName = "cubestats";
		public String dbPassword = "cubing";
		public String dbHost = "localhost";
		public int dbPort = 3306;
		public String dbSchemaStaging = "staging";
		public String dbSchemaLive = "live";
		public String dbUrlStaging = null;
		public String dbUrlLive = null;
		
		public Charset importCharset = Charset.forName("UTF-8");
		
		public int minThreadPoolSize = 4;
		public int maxThreadPoolSize = 8;
		public File resourcesRoot = null; // Must be set
		public String hostname = "localhost";
		public boolean enableRequestLogging = true;
		
		public Builder() {}
		
		public Settings build() {
			return new Settings(this);
		}
	}

}

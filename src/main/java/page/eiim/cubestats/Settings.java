package page.eiim.cubestats;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.SQLException;

public final class Settings {

	public final boolean noImport;
	public final boolean noWebserver;
	
	public final String databaseDumpUrl;
	public final String userAgent;
	public final File dataDirectory;
	public final File lastDumpMetadataFile;
	
	public final String mySQLExe;
	
	public final String dbUserName;
	public final String dbPassword;
	public final String dbHost;
	public final int dbPort;
	
	public final String dbSchemaA;
	public final String dbSchemaB;
	public final String dbUrlA;
	public final String dbUrlB;
	
	public final Charset importCharset;
	
	public final int minThreadPoolSize;
	public final int maxThreadPoolSize;
	public final File resourcesRoot;
	public final String hostname;
	public final boolean enableRequestLogging;
	
	private Settings(Builder b) {
		noImport = b.noImport;
		noWebserver = b.noWebserver;
		databaseDumpUrl = b.databaseDumpUrl;
		userAgent = b.userAgent;
		dataDirectory = b.dataDirectory;
		mySQLExe = b.mySQLExe;
		dbUserName = b.dbUserName;
		dbPassword = b.dbPassword;
		dbHost = b.dbHost;
		dbPort = b.dbPort;
		dbSchemaA = b.dbSchemaA;
		dbSchemaB = b.dbSchemaB;
		importCharset = b.importCharset;
		minThreadPoolSize = b.minThreadPoolSize;
		maxThreadPoolSize = b.maxThreadPoolSize;
		resourcesRoot = b.resourcesRoot;
		hostname = b.hostname;
		enableRequestLogging = b.enableRequestLogging;
		
		if(b.dbUrlA == null) {
			dbUrlA = "jdbc:mariadb://" + dbHost + ":" + dbPort + "/" + dbSchemaA;
		} else {
			dbUrlA = b.dbUrlA;
		}
		
		if(b.dbUrlB == null) {
			dbUrlB = "jdbc:mariadb://" + dbHost + ":" + dbPort + "/" + dbSchemaB;
		} else {
			dbUrlB = b.dbUrlB;
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
		
		public String databaseDumpUrl = "https://assets.worldcubeassociation.org/export/developer/wca-developer-database-dump.zip";
		public String userAgent = "CubeStatsBot/0.1";
		public File dataDirectory = null; // Must be set
		public File lastDumpMetadataFile = null;
		
		public String mySQLExe = "mysql";
		
		public String dbUserName = "cubestats";
		public String dbPassword = "cubing";
		public String dbHost = "localhost";
		public int dbPort = 3306;
		public String dbSchemaA = "cs_a";
		public String dbSchemaB = "cs_b";
		public String dbUrlA = null;
		public String dbUrlB = null;
		
		public Charset importCharset = Charset.forName("UTF-8");
		
		public int minThreadPoolSize = 4;
		public int maxThreadPoolSize = 8;
		public File resourcesRoot = null; // Must be set
		public String hostname = "localhost";
		public boolean enableRequestLogging = true;
		
		public Builder() {}
		
		public Settings build() throws SQLException {
			Settings s = new Settings(this);
			DatabaseConnector.initialize(s.dbUserName, s.dbPassword, s.dbUrlA, s.dbUrlB, s.dbSchemaA, s.dbSchemaB);
			return s;
		}
	}

}

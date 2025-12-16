package page.eiim.cubestats;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
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
	public DatabaseSchema liveSchema;
	public DatabaseSchema stagingSchema;
	
	private final String dbSchemaA;
	private final String dbSchemaB;
	private final String dbUrlA;
	private final String dbUrlB;
	
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
	
	public void swapDatabases() {
		DatabaseSchema temp = liveSchema;
		liveSchema = stagingSchema;
		stagingSchema = temp;
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
		public SchemaAB liveSchema = null;
		
		public Charset importCharset = Charset.forName("UTF-8");
		
		public int minThreadPoolSize = 4;
		public int maxThreadPoolSize = 8;
		public File resourcesRoot = null; // Must be set
		public String hostname = "localhost";
		public boolean enableRequestLogging = true;
		
		public Builder() {}
		
		public Settings build() {
			Settings s = new Settings(this);
			if(liveSchema == null)  {
				if(autodetectLiveStaging(s)) {
					// Correctly set by autodetectLiveStaging
				} else {
					throw new IllegalStateException("Live/staging databases could not be autodetected, please set cs_metadata tables");
				}
			}
			return s;
		}
	}
	
	public enum SchemaAB {
		A, B;
		
		public static SchemaAB fromString(String s) {
			if(s.equalsIgnoreCase("A")) {
				return A;
			} else if(s.equalsIgnoreCase("B")) {
				return B;
			} else {
				throw new IllegalArgumentException("Invalid SchemaAB string: " + s);
			}
		}
	
		public SchemaAB opposite() {
			return this == A ? B : A;
		}
	}
	
	public static boolean autodetectLiveStaging(Settings settings) {
		try {
			Connection connA = DatabaseCSN.getConnection(settings, new DatabaseSchema(settings.dbSchemaA, settings.dbUrlA));
			Connection connB = DatabaseCSN.getConnection(settings, new DatabaseSchema(settings.dbSchemaB, settings.dbUrlB));
			ResultSet rsA = connA.prepareStatement("SELECT cs_value FROM cs_metadata WHERE cs_key = \"status\"").executeQuery();
			ResultSet rsB = connB.prepareStatement("SELECT cs_value FROM cs_metadata WHERE cs_key = \"status\"").executeQuery();
			
			if(rsA.next() && rsB.next()) {
				String statusA = rsA.getString(1);
				String statusB = rsB.getString(1);
				if(statusA.equals("live") && statusB.equals("empty")) {
					settings.liveSchema = new DatabaseSchema(settings.dbSchemaA, settings.dbUrlA);
					settings.stagingSchema = new DatabaseSchema(settings.dbSchemaB, settings.dbUrlB);
				} else if(statusA.equals("empty") && statusB.equals("live")) {
					settings.liveSchema = new DatabaseSchema(settings.dbSchemaB, settings.dbUrlB);
					settings.stagingSchema = new DatabaseSchema(settings.dbSchemaA, settings.dbUrlA);
				} else {
					System.err.println("Could not autodetect live/staging databases, invalid status values: A='" + statusA + "', B='" + statusB + "'");
					return false;
				}
			} else {
				System.err.println("Could not autodetect live/staging databases, missing metadata 'status' key");
				return false;
			}
		} catch (SQLException e) {
			System.err.println("SQL error during live/staging autodetection: " + e.getMessage());
			return false;
		}
		
		return true;
	}

}

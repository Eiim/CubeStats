package page.eiim.cubestats;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Date;

public final class TaskSettings {

	public String exportMetadataUrl = "https://www.worldcubeassociation.org/api/v0/export/public";
	public Date lastExportDate = new Date(0);
	public String exportFormatVersion = "1.0.0";
	public String databaseDumpUrl = "https://assets.worldcubeassociation.org/export/developer/wca-developer-database-dump.zip";
	public String userAgent = "CubeStatsBot/0.1";
	public File dataDirectory = new File("D:\\CubeStats");
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
	
	public TaskSettings() {
		// TODO Auto-generated constructor stub
	}

}

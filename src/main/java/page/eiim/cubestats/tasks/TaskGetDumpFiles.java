package page.eiim.cubestats.tasks;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import page.eiim.cubestats.Settings;

public class TaskGetDumpFiles extends Task {

	private final String exportMetadataUrl;
	private String databaseDumpUrl;
	private final String userAgent;
	private final File dataDirectory;
	
	public TaskGetDumpFiles(Settings settings) {
		synchronized (settings) {
			exportMetadataUrl = settings.exportMetadataUrl;
			databaseDumpUrl = settings.databaseDumpUrl;
			userAgent = settings.userAgent;
			dataDirectory = settings.dataDirectory;
		}
	}

	@Override
	public String name() {
		return "Get WCA Database Dump Files";
	}

	@Override
	public void run() {
		// Get URL of database dump from metadata URL
		try {
			HttpURLConnection metadataConnection = (HttpURLConnection) new URI(exportMetadataUrl).toURL().openConnection();
			metadataConnection.setRequestProperty("User-Agent", userAgent);
			String jsonString = new String(metadataConnection.getInputStream().readAllBytes(), Charset.forName("UTF-8")); // Assume UTF-8 (probably will only ever be ASCII)
			JsonObject metadata = JsonParser.parseString(jsonString).getAsJsonObject();
			
			databaseDumpUrl = metadata.get("developer_url").getAsString();
			
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
			result = new TaskResult(false, e.getMessage());
			isDone = true;
			return;
		}
		
		System.out.println("Downloading database dump from " + databaseDumpUrl);
		try {
			HttpURLConnection dumpConnection = (HttpURLConnection) new URI(databaseDumpUrl).toURL().openConnection();
			dumpConnection.setRequestProperty("User-Agent", userAgent);
			ZipInputStream sqlZipData = new ZipInputStream(dumpConnection.getInputStream());
			ZipEntry entry;
			while((entry = sqlZipData.getNextEntry()) != null) {
				System.out.println("Extracting " + entry.getName() + " (" + entry.getSize() + " bytes)");
				File file = new File(dataDirectory, entry.getName());
				if(!file.toPath().startsWith(dataDirectory.toPath())) {
					result = new TaskResult(false, "Database dump entry is outside of data directory.");
					isDone = true;
					throw new IOException("Entry " + entry.getName() + " is outside of the data directory, possible zip slip attack.");
				}
				file.delete();
				Files.copy(sqlZipData, file.toPath());
			}
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
			result = new TaskResult(false, e.getMessage());
			isDone = true;
			return;
		}
		
		result = new TaskResult(true, "Database dump successfully downloaded.");
		isDone = true;
	}
	
	public static boolean checkForUpdate(Settings settings) {
		// Check if already up-to-date
		try {
			// HTTP HEAD request to get last modified date
			HttpURLConnection dumpConnection = (HttpURLConnection) new URI(settings.databaseDumpUrl).toURL().openConnection();
			dumpConnection.setRequestProperty("User-Agent", settings.userAgent);
			dumpConnection.setRequestMethod("HEAD");
			Map<String, List<String>> fields = dumpConnection.getHeaderFields();
			String lastModified = fields.get("Last-Modified").get(0);
			Instant lastModifiedInstant = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(lastModified));
			// Check last modified date against last download date
			if(settings.lastDumpMetadataFile.exists()) {
				String[] lastDumpMetadata = new String(Files.readAllBytes(settings.lastDumpMetadataFile.toPath()), Charset.forName("UTF-8")).split("\n");
				try {
					Instant lastDumpDate = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(lastDumpMetadata[0]));
					if(!lastModifiedInstant.isAfter(lastDumpDate)) {
						return false; // No update needed
					}
				} catch(DateTimeParseException e) {
					// Malfrormed date, redownload and overwrite file
					System.out.println("Last dump metadata file is malformed, redownloading database dump.");
				}
				lastDumpMetadata[0] = lastModified;
				Files.write(settings.lastDumpMetadataFile.toPath(), String.join("\n", lastDumpMetadata).getBytes(Charset.forName("UTF-8")));
			} else {
				Files.write(settings.lastDumpMetadataFile.toPath(), lastModified.getBytes(Charset.forName("UTF-8")));
			}
			
		} catch (IOException | URISyntaxException e1) {
			e1.printStackTrace();
			return false; // Unknown error, assume no update
		}
		
		return true; // Update needed
	}
}

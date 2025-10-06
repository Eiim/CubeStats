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

import page.eiim.cubestats.TaskSettings;

public class TaskGetDumpFiles extends Task {

	private final String databaseDumpUrl;
	private final String userAgent;
	private final File dataDirectory;
	private final File lastDumpMetadataFile;
	
	public TaskGetDumpFiles(TaskSettings settings) {
		synchronized (settings) {
			databaseDumpUrl = settings.databaseDumpUrl;
			userAgent = settings.userAgent;
			dataDirectory = settings.dataDirectory;
			lastDumpMetadataFile = settings.lastDumpMetadataFile;
		}
	}

	@Override
	public String name() {
		return "Get WCA Database Dump Files";
	}

	@Override
	public void run() {
		System.out.println("Downloading database dump from " + databaseDumpUrl);
		// Check if already up-to-date
		try {
			HttpURLConnection dumpConnection = (HttpURLConnection) new URI(databaseDumpUrl).toURL().openConnection();
			dumpConnection.setRequestProperty("User-Agent", userAgent);
			dumpConnection.setRequestMethod("HEAD");
			Map<String, List<String>> fields = dumpConnection.getHeaderFields();
			String lastModified = fields.get("Last-Modified").get(0);
			Instant lastModifiedInstant = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(lastModified));
			if(lastDumpMetadataFile.exists()) {
				String[] lastDumpMetadata = new String(Files.readAllBytes(lastDumpMetadataFile.toPath()), Charset.forName("UTF-8")).split("\n");
				try {
					Instant lastDumpDate = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(lastDumpMetadata[0]));
					if(!lastModifiedInstant.isAfter(lastDumpDate)) {
						result = new TaskResult(true, "Database dump is already up to date.");
						isDone = true;
						return;
					}
				} catch(DateTimeParseException e) {
					// Malfrormed date, redownload and overwrite file
					System.out.println("Last dump metadata file is malformed, redownloading database dump.");
				}
				lastDumpMetadata[0] = lastModified;
				Files.write(lastDumpMetadataFile.toPath(), String.join("\n", lastDumpMetadata).getBytes(Charset.forName("UTF-8")));
			} else {
				Files.write(lastDumpMetadataFile.toPath(), lastModified.getBytes(Charset.forName("UTF-8")));
			}
			
		} catch (IOException | URISyntaxException e1) {
			e1.printStackTrace();
			result = new TaskResult(false, e1.getMessage());
			isDone = true;
			return;
		}
		
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
}

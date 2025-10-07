package page.eiim.cubestats.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import page.eiim.cubestats.Settings;

public class TaskGetFiles extends Task {

	private final String exportMetadataUrl;
	private final String exportFormatVersion;
	private final String userAgent;
	private final File dataDirectory;
	
	private static final SimpleDateFormat localMetadataDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	
	public TaskGetFiles(Settings settings) {
		synchronized (settings) {
			exportMetadataUrl = settings.exportMetadataUrl;
			exportFormatVersion = "1.0.0";
			userAgent = settings.userAgent;
			dataDirectory = settings.dataDirectory;
		}
	}

	@Override
	public String name() {
		return "Get WCA Export Files";
	}

	@Override
	public void run() {
		Gson g = new Gson();
		Date lastExportDate;
		
		try {
			File metadataFile = new File(dataDirectory, "metadata.json");
			if (metadataFile.exists()) {
				ExportFileMetadata efm = g.fromJson(Files.readString(metadataFile.toPath(), Charset.forName("UTF-8")), ExportFileMetadata.class);
				if (!efm.export_format_version.equals(exportFormatVersion)) {
					result = new TaskResult(false, "Export format version mismatch in existing metadata.json: expected " + exportFormatVersion + ", got " + efm.export_format_version);
					isDone = true;
					return;
				}
				lastExportDate = localMetadataDateFormat.parse(efm.export_date);
			} else {
				lastExportDate = new Date(0); // If no metadata file exists, we should definitely download
			}
		} catch (JsonSyntaxException | IOException | ParseException e) {
			e.printStackTrace();
			result = new TaskResult(false, e.getMessage());
			isDone = true;
			return;
		}
		
		BufferedReader reader;
		try {
			URLConnection metadataConnection = new URI(exportMetadataUrl).toURL().openConnection();
			metadataConnection.setRequestProperty("User-Agent", userAgent);
			reader = new BufferedReader(new InputStreamReader(metadataConnection.getInputStream(), Charset.forName("UTF-8")));
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = new TaskResult(false, e.getMessage());
			isDone = true;
			return;
		}
		
		ExportMetadata em = g.fromJson(reader, ExportMetadata.class);
		if(!em.export_date.after(lastExportDate)) {
			result = new TaskResult(true, "No new export available.");
			isDone = true;
			return;
		}
		
		try {
			URLConnection metadataConnection = new URI(em.tsv_url).toURL().openConnection();
			metadataConnection.setRequestProperty("User-Agent", userAgent);
			ZipInputStream tsvZipData = new ZipInputStream(metadataConnection.getInputStream());
			ZipEntry entry;
			while((entry = tsvZipData.getNextEntry()) != null) {
				File file = new File(dataDirectory, entry.getName());
				file.delete();
				Files.copy(tsvZipData, file.toPath());
			}
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = new TaskResult(false, e.getMessage());
			isDone = true;
			return;
		}
		
		File metadataFile = new File(dataDirectory, "metadata.json");
		try {
			ExportFileMetadata efm = g.fromJson(Files.readString(metadataFile.toPath(), Charset.forName("UTF-8")), ExportFileMetadata.class);
			if (!efm.export_format_version.equals(exportFormatVersion)) {
				result = new TaskResult(false, "Export format version mismatch: expected " + exportFormatVersion + ", got " + efm.export_format_version);
				isDone = true;
				return;
			}
		} catch (JsonSyntaxException | IOException e) {
			e.printStackTrace();
			result = new TaskResult(false, e.getMessage());
			isDone = true;
			return;
		}
		
		result = new TaskResult(true, "Export files successfully downloaded.");
		isDone = true;
	}
	
	private record ExportMetadata(
			Date export_date,
			String sql_url, String sql_filesize_bytes,
			String tsv_url, String tsv_filesize_bytes,
			String developer_url,
			String readme) {}
	
	private record ExportFileMetadata(String export_format_version, String export_date) {}
}

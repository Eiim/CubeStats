package page.eiim.cubestats.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Date;

import com.google.gson.Gson;

import page.eiim.cubestats.TaskSettings;

/*
 * This class should probably only be used for testing. Regular update checks should be handled outside of the Task framework.
 */
public class TaskCheckUpdate extends Task {

	private final String exportMetadataUrl;
	private final Date lastExportDate;
	private final String userAgent;
	
	public TaskCheckUpdate(TaskSettings settings) {
		synchronized (settings) {
			exportMetadataUrl = settings.exportMetadataUrl;
			lastExportDate = settings.lastExportDate;
			userAgent = settings.userAgent;
		}
	}

	@Override
	public String name() {
		return "Check WCA files update";
	}

	@Override
	public void run() {
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
		
		Gson g = new Gson();
		ExportMetadata em = g.fromJson(reader, ExportMetadata.class);
		if(!em.export_date.after(lastExportDate)) {
			result = new TaskResult(true, "No new export available.");
			isDone = true;
		} else {
			result = new TaskResult(true, "New export available.");
			isDone = true;
		}
	}
	
	private record ExportMetadata(
			Date export_date,
			String sql_url, String sql_filesize_bytes,
			String tsv_url, String tsv_filesize_bytes,
			String developer_url,
			String readme) {}
}

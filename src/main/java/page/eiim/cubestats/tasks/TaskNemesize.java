package page.eiim.cubestats.tasks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import page.eiim.cubestats.DatabaseCSN;
import page.eiim.cubestats.Settings;

public class TaskNemesize extends Task {
	
	public TaskNemesize(Settings settings) {
		super(settings);
	}
	
	@Override
	public String name() {
		return "Prepare Nemeses";
	}

	@Override
	public void run() {
		try {
			Connection conn = DatabaseCSN.getConnection(settings, stagingSchema);
			
			conn.prepareStatement("DROP TABLE IF EXISTS cs_nemeses").executeUpdate();
			conn.prepareStatement("CREATE TABLE cs_nemeses (nemesizer INT NOT NULL, nemesizee INT NOT NULL)").executeUpdate();
			
			// Get all active events
			ResultSet eventsRS = conn.prepareStatement("SELECT id FROM cs_events WHERE is_active").executeQuery();
			ArrayList<String> events = new ArrayList<>();
			while(eventsRS.next()) {
				events.add(eventsRS.getString(1));
			}
			
			String eventsString = String.join("','", events);
			ResultSet times = conn.prepareStatement(
					"SELECT best, person_id, event_id, FALSE as average FROM ranks_single WHERE event_id IN ('"+eventsString+"') " +
					"UNION SELECT best, person_id, event_id, TRUE as average FROM ranks_average WHERE event_id IN ('"+eventsString+"') " +
					"ORDER BY person_id").executeQuery();
			
			Map<Long, ArrayList<String>> categoryMap = new HashMap<>();
			String currentPerson = "";
			long currentFlag = 0;
			while(times.next())  {
				String person = times.getString(1);
				int time = times.getInt(1);
				String event = times.getString(3);
				boolean isAverage = times.getBoolean(4);
				
				if(!person.equals(currentPerson)) {
					if(!currentPerson.isEmpty()) {
						categoryMap.computeIfAbsent(currentFlag, _ -> new ArrayList<>());
						categoryMap.get(currentFlag).add(currentPerson);
					}
					currentPerson = person;
					currentFlag = 0;
				} else {
					int eIndex = events.indexOf(event) + (isAverage ? events.size() : 0);
					currentFlag |= 1 << eIndex;
				}
			}
			
			ArrayList<Long> categories = new ArrayList<>(categoryMap.keySet());
			int totalCompetitors = 0;
			double totalRemove = 0;
			double totalInclude = 0;
			for(Long a : categories) {
				double removeCount = 0;
				double includeCount = 0;
				for(Long b : categories) {
					if((a & b) == b || (a & b) == a) { // A nemesizes B or B nemesizes A
						removeCount += categoryMap.get(b).size();
					} else {
						includeCount += categoryMap.get(b).size();
					}
				}
				totalCompetitors += categoryMap.get(a).size();
				totalRemove += removeCount * categoryMap.get(a).size();
				totalInclude += includeCount * categoryMap.get(a).size();
			}
			System.out.println("Average remove: " + (totalRemove/totalCompetitors));
			System.out.println("Average include: " + (totalInclude/totalCompetitors));
			
			result = new TaskResult(true, "Finished calculating nemeses");
			isDone = true;
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			result = new TaskResult(false, e.getMessage());
			isDone = true;
			return;
		}
	}
}

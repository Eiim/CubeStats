package page.eiim.cubestats.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class CubeSearch {

	private TreeSet<String> eventIds;
	private TreeSet<String> competitionIds;
	private TreeSet<WCAId> personIds;
	private WordStorage wordStoragePersons;
	private WordStorage wordStorageCompetitions;
	
	public CubeSearch(Connection conn) throws SQLException {
		eventIds = new TreeSet<>();
		competitionIds = new TreeSet<>();
		personIds = new TreeSet<>();
		List<String> personNames = new ArrayList<>();
		List<String> competitionNames = new ArrayList<>();
		List<String> personIdsList = new ArrayList<>();
		List<String> competitionIdsList = new ArrayList<>();
		
		var stmt = conn.createStatement();
		var rs = stmt.executeQuery("SELECT id FROM Events");
		while(rs.next()) {
			eventIds.add(rs.getString(1).toLowerCase(Locale.ROOT));
		}
		rs.close();
		stmt.close();
		
		stmt = conn.createStatement();
		rs = stmt.executeQuery("SELECT id, name FROM Competitions");
		while(rs.next()) {
			competitionIds.add(rs.getString(1).toLowerCase(Locale.ROOT));
			competitionIdsList.add(rs.getString(1).toLowerCase(Locale.ROOT));
			competitionNames.add(rs.getString(2).toLowerCase(Locale.ROOT));
		}
		rs.close();
		stmt.close();
		
		stmt = conn.createStatement();
		rs = stmt.executeQuery("SELECT wca_id, name FROM Persons");
		while(rs.next()) {
			personIds.add(new WCAId(rs.getString(1)));
			personIdsList.add(rs.getString(1).toLowerCase(Locale.ROOT));
			personNames.add(rs.getString(2).toLowerCase(Locale.ROOT));
		}
		rs.close();
		stmt.close();
		
		wordStoragePersons = new WordStorage(personNames, personIdsList);
		wordStorageCompetitions = new WordStorage(competitionNames, competitionIdsList);
	}
	
	public List<QueryResult> query(String query, int maxResults) {
		return query(query, maxResults, true, true, true);
	}
	
	public List<QueryResult> query(String query, int maxResults, boolean includePersons, boolean includeCompetitions, boolean includeEvents) {
		query = query.toLowerCase(Locale.ROOT);
		List<QueryResult> queryResults = new ArrayList<>(maxResults);
		
		// Exact ID matches
		if(includeEvents && eventIds.contains(query)) {
			queryResults.add(new QueryResult(query, ResultType.EVENT));
		}
		if(includePersons && WCAId.isValid(query) && personIds.contains(new WCAId(query))) {
			queryResults.add(new QueryResult(query.toUpperCase(), ResultType.PERSON));
		}
		if(includeCompetitions && competitionIds.contains(query)) {
			queryResults.add(new QueryResult(query, ResultType.COMPETITION));
		}
		if(queryResults.size() >= maxResults) return queryResults.subList(0, maxResults); // Likely don't need sublist, but just in case
		
		// Look for ID prefix matches
		if(includeEvents) {
			String higherId = eventIds.higher(query);
			while(queryResults.size() < maxResults && higherId != null && higherId.startsWith(query)) {
				queryResults.add(new QueryResult(higherId, ResultType.EVENT));
				higherId = eventIds.higher(higherId);
			}
			
		}
		
		if(includeCompetitions) {
			String higherId = competitionIds.higher(query);
			while(queryResults.size() < maxResults && higherId != null && higherId.startsWith(query)) {
				queryResults.add(new QueryResult(higherId, ResultType.COMPETITION));
				higherId = competitionIds.higher(higherId);
			}
		}
		
		if(includePersons && query.length() <= 10) {
			try {
				WCAId higherWcaId = personIds.ceiling(WCAId.minPrefix(query));
				while(queryResults.size() < maxResults && higherWcaId != null && higherWcaId.toString().startsWith(query)) {
					queryResults.add(new QueryResult(higherWcaId.toString(), ResultType.PERSON));
					higherWcaId = personIds.higher(higherWcaId);
				}
			} catch(Exception e) {
				// Probably an invalid WCA ID prefix, ignore
			}
		}
		
		if(includePersons && queryResults.size() < maxResults) {
			List<String> personResults = wordStoragePersons.query(query, maxResults - queryResults.size());
			for(String personResult : personResults) {
				if(queryResults.size() >= maxResults) break;
				queryResults.add(new QueryResult(personResult, ResultType.PERSON));
			}
		}
		
		if(includeCompetitions && queryResults.size() < maxResults) {
			List<String> competitionResults = wordStorageCompetitions.query(query, maxResults - queryResults.size());
			for(String competitionResult : competitionResults) {
				if(queryResults.size() >= maxResults) break;
				queryResults.add(new QueryResult(competitionResult, ResultType.COMPETITION));
			}
		}
		
		return queryResults;
	}
	
	public static enum ResultType {
		EVENT,
		COMPETITION,
		PERSON;
		
		private String jsonName;
		
		private ResultType() {
			jsonName = this.toString().toLowerCase();
		}
		
		public String jsonName() {
			return jsonName;
		}
	}
	
	public static record QueryResult(String result, ResultType type) {}
	
	public static void main(String[] args) {
		CubeSearch cs;
		try {
			Connection conn = DatabaseConnector.getConnection("cubestats", "cubing", "jdbc:mariadb://localhost:3306/live");
			cs = new CubeSearch(conn);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		List<QueryResult> result;
		
		long start = System.nanoTime();
		result = cs.query("333", 10);
		long end = System.nanoTime();
		System.out.println("Query '333' took " + (end - start) / 1_000_000.0 + " ms");
		result.forEach(r -> System.out.println(r.type + ": " + r.result));
		
		// WC2025
		start = System.nanoTime();
		result = cs.query("WC2025", 10);
		end = System.nanoTime();
		System.out.println("Query 'WC2025' took " + (end - start) / 1_000_000.0 + " ms");
		result.forEach(r -> System.out.println(r.type + ": " + r.result));

		// 2024CHAP08
		start = System.nanoTime();
		result = cs.query("2024CHAP08", 10);
		end = System.nanoTime();
		System.out.println("Query '2024CHAP08' took " + (end - start) / 1_000_000.0 + " ms");
		result.forEach(r -> System.out.println(r.type + ": " + r.result));

		// Ethan Chapman
		start = System.nanoTime();
		result = cs.query("Ethan Chapman", 10);
		end = System.nanoTime();
		System.out.println("Query 'Ethan Chapman' took " + (end - start) / 1_000_000.0 + " ms");
		result.forEach(r -> System.out.println(r.type + ": " + r.result));

		// Some Random Name
		start = System.nanoTime();
		result = cs.query("Some Random Name", 10);
		end = System.nanoTime();
		System.out.println("Query 'Some Random Name' took " + (end - start) / 1_000_000.0 + " ms");
		result.forEach(r -> System.out.println(r.type + ": " + r.result));

		// Harsha
		start = System.nanoTime();
		result = cs.query("Harsha", 10);
		end = System.nanoTime();
		System.out.println("Query 'Harsha' took " + (end - start) / 1_000_000.0 + " ms");
		result.forEach(r -> System.out.println(r.type + ": " + r.result));

		// Paladugu
		start = System.nanoTime();
		result = cs.query("Paladugu", 10);
		end = System.nanoTime();
		System.out.println("Query 'Paladugu' took " + (end - start) / 1_000_000.0 + " ms");
		result.forEach(r -> System.out.println(r.type + ": " + r.result));
		
		// Daniel
		start = System.nanoTime();
		result = cs.query("Daniel", 10);
		end = System.nanoTime();
		System.out.println("Query 'Daniel' took " + (end - start) / 1_000_000.0 + " ms");
		result.forEach(r -> System.out.println(r.type + ": " + r.result));
		
		// Dani
		start = System.nanoTime();
		result = cs.query("Dani", 10);
		end = System.nanoTime();
		System.out.println("Query 'Dani' took " + (end - start) / 1_000_000.0 + " ms");
		result.forEach(r -> System.out.println(r.type + ": " + r.result));
		
	}

}

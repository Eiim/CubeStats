package page.eiim.cubestats.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class CubeSearch {

	private HashSet<String> eventIds;
	private HashSet<String> competitionIds;
	private HashSet<WCAId> personIds;
	private WordStorage wordStorage;
	private Connection conn;
	
	public CubeSearch(Connection conn) throws SQLException {
		this.conn = conn;
		eventIds = new HashSet<>();
		competitionIds = new HashSet<>();
		personIds = new HashSet<>();
		List<String> personCompNames = new ArrayList<>();
		
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
			personCompNames.add(rs.getString(2).toLowerCase(Locale.ROOT));
		}
		rs.close();
		stmt.close();
		
		stmt = conn.createStatement();
		rs = stmt.executeQuery("SELECT wca_id, name FROM Persons");
		while(rs.next()) {
			personIds.add(new WCAId(rs.getString(1)));
			personCompNames.add(rs.getString(2).toLowerCase(Locale.ROOT));
		}
		rs.close();
		stmt.close();
		
		wordStorage = new WordStorage(personCompNames);
	}
	
	public List<QueryResult> query(String query, int maxResults) {
		query = query.toLowerCase(Locale.ROOT);
		if(eventIds.contains(query)) {
			return List.of(new QueryResult(query, ResultType.EVENT));
		} else if(query.length() == 10 && personIds.contains(new WCAId(query))) {
			return List.of(new QueryResult(query, ResultType.PERSON));
		} else if(competitionIds.contains(query)) {
			return List.of(new QueryResult(query, ResultType.COMPETITION));
		} else {
			List<String> results = wordStorage.query(query.toLowerCase(Locale.ROOT), maxResults);
			if(results.size() == 0) return List.of();
			List<QueryResult> queryResults = new ArrayList<>(results.size());
			// Query database to determine if result is a person or competition and get corresponding id
			try {
				PreparedStatement ps1 = conn.prepareStatement("SELECT id FROM competitions WHERE name = ?");
				PreparedStatement ps2 = conn.prepareStatement("SELECT wca_id FROM persons WHERE name = ?");
				for(String result : results) {
					ps1.setString(1, result);
					var rs = ps1.executeQuery();
					if(rs.next()) {
						queryResults.add(new QueryResult(rs.getString(1), ResultType.COMPETITION));
						rs.close();
						continue;
					}
					rs.close();
					
					ps2.setString(1, result);
					rs = ps2.executeQuery();
					if(rs.next()) {
						queryResults.add(new QueryResult(rs.getString(1), ResultType.PERSON));
						rs.close();
						continue;
					}
					rs.close();
				}
				ps1.close();
				ps2.close();
				return queryResults;
			} catch (SQLException e) {
				e.printStackTrace();
				return List.of();
			}
		}
	}
	
	private enum ResultType {
		EVENT,
		COMPETITION,
		PERSON,
	}
	
	private static record QueryResult(String result, ResultType type) {}
	
	public static void main(String[] args) {
		CubeSearch cs;
		try {
			Connection conn = DatabaseConnector.getConnection("cubestats", "cubing", "jdbc:mariadb://localhost:3306/staging");
			cs = new CubeSearch(conn);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		//System.out.println(WordStorage.getWordInt("chapman"));
		
		//cs.query("Chapman", 10).forEach(r -> System.out.println(r.type + ": " + r.result));
		
		
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

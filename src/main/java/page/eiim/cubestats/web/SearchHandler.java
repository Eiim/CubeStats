package page.eiim.cubestats.web;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import page.eiim.cubestats.web.CubeSearch.QueryResult;

public class SearchHandler extends Handler.Abstract.NonBlocking {
	
	private final CubeSearch cubeSearch;
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	public SearchHandler(CubeSearch cubeSearch) {
		this.cubeSearch = cubeSearch;
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		if(!"/searchapi".equals(request.getHttpURI().getPath())) {
			return false;
		}
		String queryString = request.getHttpURI().getQuery();
		if(queryString == null || queryString.isBlank()) {
			response.setStatus(400);
			response.write(true, ByteBuffer.wrap("Need URL paramters".getBytes(UTF8)), callback);
			return true;
		}
		
		// Parse URL parameters
		String[] params = queryString.split("&");
		String query = null;
		int maxResults = 10;
		String form = "json";
		boolean includePersons = true;
		boolean includeCompetitions = true;
		boolean includeEvents = true;
		for(String param : params) {
			String[] parts = param.split("=", 2);
			if(parts.length == 2) {
				switch(parts[0]) {
					case "q" -> query = URLDecoder.decode(parts[1], UTF8);
					case "n" -> {
						try {
							maxResults = Integer.parseInt(parts[1]);
						} catch(NumberFormatException e) {
							response.setStatus(400);
							response.write(true, ByteBuffer.wrap("Invalid URL paramter n".getBytes(UTF8)), callback);
							return true;
						}
					}
					case "f" -> form = parts[1];
					case "t" -> {
						switch(parts[1]) {
							case "person" -> {includeCompetitions = false; includeEvents = false;}
							case "competition" -> {includePersons = false; includeEvents = false;}
							case "event" -> {includePersons = false; includeCompetitions = false;}
							default -> {
								response.setStatus(400);
								response.write(true, ByteBuffer.wrap("Invalid URL paramter t".getBytes(UTF8)), callback);
								return true;
							}
						}
					}
				}
			}
		}
		if(query == null || query.isBlank()) {
			response.setStatus(400);
			response.write(true, ByteBuffer.wrap("Need URL paramter q".getBytes(UTF8)), callback);
			return true;
		}
		
		boolean redirectURL = false;
		if("wca".equals(form)) {
			redirectURL = true;
			form = "wca_url";
		}
		
		boolean eventAverage = false;
		boolean eventRecords = false;
		if(redirectURL) {
			maxResults = 1;
			if(query.length() > 2 && query.charAt(1) == '!') {
				switch(query.charAt(0)) {
					case 'a' -> eventAverage = true;
					case 'r' -> eventRecords = true;
					case 'p' -> {includePersons = true; includeCompetitions = false; includeEvents = false;}
					case 'c' -> {includeCompetitions = true; includePersons = false; includeEvents = false;}
					case 'e' -> {includeEvents = true; includePersons = false; includeCompetitions = false;}
					default -> {
						// Ignore invalid prefix
					}
				}
				query = query.substring(2);
			}
		}
		maxResults = Math.min(Math.max(maxResults, 1), 100);
		
		List<QueryResult> results = cubeSearch.query(query, maxResults, includePersons, includeCompetitions, includeEvents);
		String resultString = switch(form) {
			case "wca_url" -> {
				if(results.size() == 0) {
					yield "https://www.worldcubeassociation.org/";
				}
				
				StringBuilder sb = new StringBuilder();
				for(int i = 0; i < results.size(); i++) {
					QueryResult r = results.get(i);
					switch(r.type()) {
						case PERSON -> sb.append("https://www.worldcubeassociation.org/persons/").append(r.result());
						case COMPETITION -> sb.append("https://www.worldcubeassociation.org/competitions/").append(r.result());
						case EVENT -> {
							if(eventRecords) {
								sb.append("https://www.worldcubeassociation.org/results/records?event_id=").append(r.result());
							} else if(eventAverage) {
								sb.append("https://www.worldcubeassociation.org/results/rankings/").append(r.result()).append("/average");
							} else {
								sb.append("https://www.worldcubeassociation.org/results/rankings/").append(r.result()).append("/single");
							}
						}
					};
					
					if(i < results.size() - 1) {
						sb.append('\n');
					}
				}
				
				yield sb.toString();
			}
			default -> {
				// Default to JSON
				StringBuilder sb = new StringBuilder();
				sb.append('[');
				
				for(int i = 0; i < results.size(); i++) {
					QueryResult r = results.get(i);
					sb.append("{\"result\":\"");
					sb.append(r.result()); // No need to escape, IDs don't contain special characters
					sb.append("\",\"type\":\"");
					sb.append(r.type().jsonName());
					sb.append("\"}");
					if(i < results.size() - 1) {
						sb.append(',');
					}
				}
				
				sb.append(']');
				yield sb.toString();
			}
		};
		
		if(redirectURL) {
			response.setStatus(303);
			response.getHeaders().put("Location", resultString);
			response.write(true, null, callback);
		} else {
			response.setStatus(200);
			response.getHeaders().put("Content-Type", "application/json");
			response.write(true, ByteBuffer.wrap(resultString.getBytes(UTF8)), callback);
		}
		
		return true;
	}

}

package page.eiim.cubestats.web;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import page.eiim.cubestats.tasks.TaskBayesEval.Parameters;
import page.eiim.cubestats.web.PageBuilder.PersonData;
import page.eiim.cubestats.web.PageBuilder.ResourceCategory;

public class PersonHandler extends Handler.Abstract.NonBlocking {
	
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private Connection conn;
	private Set<String> validEventIds = new HashSet<>();
	
	public PersonHandler(Connection conn) {
		this.conn = conn;
		
		try {
			PreparedStatement ps = conn.prepareStatement("SELECT id, rank FROM events");
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				int rank = rs.getInt(2);
				if(rank < 900) {
					validEventIds.add(rs.getString(1));
				}
			}
		} catch(SQLException e) {
			e.printStackTrace();
			validEventIds = Set.of("222", "333", "444", "555", "666", "777", "333bf", "444bf", "555bf", "333fm", "333oh", "clock", "minx", "pyram", "skewb", "sq1");
		}
	}
	
	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		String path = request.getHttpURI().getPath();
		if(!path.startsWith("/person/") || !(path.length() == 18)) {
			return false;
		}
		
		String personId = path.substring(8, 18).toUpperCase();
		
		String personName = null;
		PreparedStatement ps1 = conn.prepareStatement("SELECT name, wca_id FROM persons WHERE wca_id=? ORDER BY sub_id DESC LIMIT 1");
		ps1.setString(1, personId);
		ResultSet rs1 = ps1.executeQuery();
		if(!rs1.next()) {
			response.setStatus(404);
			response.write(true, ByteBuffer.wrap("Person not found".getBytes(UTF8)), callback);
			rs1.close();
			ps1.close();
			return true;
		} else {
			personName = rs1.getString(1);
			personId = rs1.getString(2); // get the canonical WCA ID, hopefully same as input
		}
		
		String personURL = "/noavatar.png";
		PreparedStatement ps2 = conn.prepareStatement("SELECT user_avatars.backend, user_avatars.filename FROM users JOIN user_avatars ON user_avatars.id = users.id WHERE users.wca_id=?");
		ps2.setString(1, personId);
		ResultSet rs2 = ps2.executeQuery();
		if(rs2.next()) {
			String backend = rs2.getString(1);
			String filename = rs2.getString(2);
			if(backend.equals("s3-legacy-cdn")) {
				personURL = "https://avatars.worldcubeassociation.org/uploads/user/avatar/"+personId+"/"+filename;
			} else {
				// Can't handle other backends yet. Might need to reach out to WST for help
			}
		}
		
		PreparedStatement ps3 = conn.prepareStatement("SELECT event_id, mu_a, m, alpha, theta, alep, bet FROM cs_bayes_params WHERE person_id=?");
		ps3.setString(1, personId);
		ResultSet rs3 = ps3.executeQuery();
		Map<String, Parameters> eventParams = new HashMap<>();
		while(rs3.next()) {
			String eventId = rs3.getString(1);
			if(!validEventIds.contains(eventId)) {
				continue;
			}
			Parameters params = new Parameters(
					rs3.getDouble(2),
					rs3.getDouble(3),
					rs3.getDouble(4),
					rs3.getDouble(5),
					rs3.getDouble(6),
					rs3.getDouble(7)
			);
			eventParams.put(eventId, params);
		}
		
		PersonData pd = new PersonData(personId, personName, personURL, eventParams);
		
		PageBuilder.Instance pb = PageBuilder.getInstance(pd);
		String html = pb
				.buildHead(null, null, ResourceCategory.PERSON)
				.addSidebar()
				.startBody()
				.addLogo()
				.enterMain()
				.addPersonBody()
				.signAndClose()
				.build();
		
		response.setStatus(200);
		response.write(true, ByteBuffer.wrap(html.getBytes(UTF8)), callback);
		
		return true;
	}

}

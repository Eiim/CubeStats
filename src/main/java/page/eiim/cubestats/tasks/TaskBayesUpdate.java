package page.eiim.cubestats.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import page.eiim.cubestats.DatabaseCSN;
import page.eiim.cubestats.Settings;

public class TaskBayesUpdate extends Task {
	
	private static final double A = 0.2;
	private static final double B = 15;
	
	public TaskBayesUpdate(Settings settings) {
		super(settings);
	}
	
	@Override
	public String name() {
		return "Update Bayesian parameters";
	}

	@Override
	public void run() {
		try {
			Connection conn = DatabaseCSN.getConnection(settings, DatabaseCSN.DefaultSchema.STAGING);
			conn.setAutoCommit(false);
			
			conn.prepareStatement("DROP TABLE IF EXISTS cs_bayes_params").executeUpdate();
			
			conn.prepareStatement("CREATE TABLE cs_bayes_params (person_id VARCHAR(10), event_id VARCHAR(6), mu_a DOUBLE, m DOUBLE, alpha DOUBLE, theta DOUBLE, alep DOUBLE, bet DOUBLE)").executeUpdate();
			
			ResultSet rsEvents = conn.prepareStatement("SELECT event_id, mu_a_0, m_0, alpha_0, theta_0, alep_0, bet_0 FROM cs_bayes_priors").executeQuery();
			Map<String, Parameters> priors = new TreeMap<>(); // Is TreeMap best here?
			
			while(rsEvents.next()) {
				String event = rsEvents.getString(1);
				priors.put(event, new Parameters(
						rsEvents.getDouble(2),
						rsEvents.getDouble(3),
						rsEvents.getDouble(4),
						rsEvents.getDouble(5),
						rsEvents.getDouble(6),
						rsEvents.getDouble(7)
				));
			}
			
			ResultSet rs = conn.prepareStatement("SELECT results.person_id, results.event_id, competitions.end_date, results.value1, results.value2, results.value3, results.value4, results.value5\r\n"
					+ "FROM results\r\n"
					+ "JOIN competitions ON results.competition_id = competitions.id\r\n"
					+ "ORDER BY person_id, event_id, competitions.end_date ASC").executeQuery();
			
			System.out.println("Getting results...");
			
			String currentPerson = "";
			String currentEvent = "";
			ArrayList<Double> times = new ArrayList<>();
			ArrayList<LocalDate> dates = new ArrayList<>();
			int dnfs = 0;
			boolean validEvent = false;
			ArrayList<String> personsToWrite = new ArrayList<>();
			ArrayList<String> eventsToWrite = new ArrayList<>();
			ArrayList<Parameters> paramsToWrite = new ArrayList<>();
			while(rs.next()) {
				String person = rs.getString(1);
				String event = rs.getString(2);
				if(!event.equals(currentEvent) || !person.equals(currentPerson)) {
					if(!times.isEmpty()) {
						// Process previous event
						Parameters prior = priors.get(currentEvent);
						int n = times.size();
						double[] weights = calculateWeights(dates);
						double sumWeights = 0;
						for(double w : weights) sumWeights += w;
						double weightedMean = 0;
						for(int i = 0; i < n; i++) {
							weightedMean += weights[i] * times.get(i);
						}
						weightedMean /= sumWeights;
						double weightedNVariance = 0;
						for(int i = 0; i < n; i++) {
							weightedNVariance += weights[i] * (times.get(i) - weightedMean) * (times.get(i) - weightedMean);
						}
						double m_b = sumWeights + prior.m;
						Parameters p = new Parameters(
								(sumWeights/m_b)*weightedMean + (prior.m/m_b)*prior.mu_a,
								m_b,
								prior.alpha + (sumWeights/2),
								prior.theta + (weightedNVariance/2) + (sumWeights*prior.m)/(2*m_b),
								prior.alep + dnfs,
								prior.bet + n);
						// Write to database
						personsToWrite.add(currentPerson);
						eventsToWrite.add(currentEvent);
						paramsToWrite.add(p);
					}
					validEvent = priors.containsKey(event);
					currentEvent = event;
					currentPerson = person;
					times.clear();
					dates.clear();
					dnfs = 0;
				}
				if(!validEvent) continue;
				LocalDate date = rs.getObject(3, LocalDate.class);
				for(int i = 4; i <= 8; i++) {
					int time = rs.getInt(i);
					if(time > 0) {
						times.add(Math.log(time));
						dates.add(date);
					} else if(time == -1) { // DNF
						dnfs++;
					}
				}
			}
			
			System.out.println("Writing "+personsToWrite.size()+" parameter sets...");
			
			PreparedStatement ps = conn.prepareStatement("INSERT INTO cs_bayes_params (person_id, event_id, mu_a, m, alpha, theta, alep, bet) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
			
			for(int i = 0; i < personsToWrite.size(); i++) {
				ps.setString(1, personsToWrite.get(i));
				ps.setString(2, eventsToWrite.get(i));
				Parameters p = paramsToWrite.get(i);
				ps.setDouble(3, p.mu_a);
				ps.setDouble(4, p.m);
				ps.setDouble(5, p.alpha);
				ps.setDouble(6, p.theta);
				ps.setDouble(7, p.alep);
				ps.setDouble(8, p.bet);
				ps.addBatch();
				if(i % 100 == 0 || i == personsToWrite.size() - 1) {
					ps.executeBatch();
					conn.commit();
				}
			}
			
			result = new TaskResult(true, "Finished updating Bayesian parameters");
			isDone = true;
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			result = new TaskResult(false, e.getMessage());
			isDone = true;
			return;
		}
	}
	
	private static double[] calculateWeights(ArrayList<LocalDate> dates) {
		long mostRecent = dates.get(dates.size() - 1).toEpochDay();
		double[] weights = new double[dates.size()];
		//ArrayList<Double> weights = new ArrayList<>(dates.size());
		double sum = 0;
		for(int i = 0; i < dates.size(); i++) {
			long days = mostRecent - dates.get(i).toEpochDay();
			//int solves = dates.size() - i;
			//double weight = days == 0 ? 1.0 : 0.0;
			double weight = 1.0 / (1.0 + Math.exp(A*(days - B)));
			sum += weight;
			weights[i] = weight;
		}
		// Normalize weights
		for(int i = 0; i < weights.length; i++) {
			weights[i] /= sum;
		}
		
		return weights;
	}
	
	private static record Parameters(double mu_a, double m, double alpha, double theta, double alep, double bet) {}
}

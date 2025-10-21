package page.eiim.cubestats.tasks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import page.eiim.cubestats.DatabaseCSN;
import page.eiim.cubestats.Settings;

public class TaskBayesPriors extends Task {
	
	public TaskBayesPriors(Settings settings) {
		super(settings);
	}
	
	@Override
	public String name() {
		return "Calculate Bayesian priors";
	}

	@Override
	public void run() {
		try {
			Connection conn = DatabaseCSN.getConnection(settings, stagingSchema);
			
			conn.prepareStatement("DROP TABLE IF EXISTS cs_bayes_priors").executeUpdate();
			
			ResultSet rsEvents = conn.prepareStatement("SELECT id, format FROM cs_events").executeQuery();
			Map<String, ArrayList<Double>> means = new TreeMap<>(); // Is TreeMap best here?
			Map<String, ArrayList<Double>> variances = new TreeMap<>();
			Map<String, ArrayList<Double>> dnfRates = new TreeMap<>();
			Set<String> eventsToSkip = new HashSet<>();
			
			while(rsEvents.next()) {
				String event = rsEvents.getString(1);
				String format = rsEvents.getString(2);
				if(!"time".equals(format)) {
					eventsToSkip.add(event);
					continue;
				}
				means.put(event, new ArrayList<>());
				variances.put(event, new ArrayList<>());
				dnfRates.put(event, new ArrayList<>());
			}
			
			ResultSet rs = conn.prepareStatement("SELECT person_id, event_id, value1, value2, value3, value4, value5 FROM results ORDER BY person_id, event_id ASC").executeQuery();
			
			System.out.println("Getting results...");
			
			String currentPerson = "";
			String currentEvent = "";
			ArrayList<Double> times = new ArrayList<>();
			int dnfs = 0;
			while(rs.next()) {
				String person = rs.getString(1);
				String event = rs.getString(2);
				if(!event.equals(currentEvent) || !person.equals(currentPerson)) {
					if(!times.isEmpty() && !eventsToSkip.contains(currentEvent)) {
						// Process previous event
						double sum = 0;
						for(double t : times) {
							sum += t;
						}
						double mean = sum / times.size();
						double varianceSum = 0;
						for(double t : times) {
							varianceSum += (t - mean) * (t - mean);
						}
						double variance = varianceSum / (times.size() - 1);
						double dnfRate = (double)dnfs / (times.size() + dnfs);
						
						if(times.size() >= 5 && variance > .01) { // Restrict to moderate samples with some variance
							means.get(currentEvent).add(mean);
							variances.get(currentEvent).add(variance);
						}
						if(times.size() + dnfs >= 100) { // Restrict to large samples
							dnfRates.get(currentEvent).add(dnfRate);
						}
					}
					currentEvent = event;
					currentPerson = person;
					times.clear();
					dnfs = 0;
				}
				for(int i = 3; i <= 7; i++) {
					int time = rs.getInt(i);
					if(time > 0) {
						times.add(Math.log(time));
					} else if(time == -1) { // DNF
						dnfs++;
					}
				}
			}
			
			System.out.println("Calculating priors...");
			
			conn.prepareStatement("CREATE TABLE cs_bayes_priors (event_id VARCHAR(6) PRIMARY KEY, mu_a_0 DOUBLE, m_0 DOUBLE, alpha_0 DOUBLE, theta_0 DOUBLE, alep_0 DOUBLE, bet_0 DOUBLE)").executeUpdate();
			
			for(String event : means.keySet()) {
				System.out.println("Calculating priors for " + event);
				ArrayList<Double> eventMeans = means.get(event);
				ArrayList<Double> eventVariances = variances.get(event);
				ArrayList<Double> eventDnfRates = dnfRates.get(event);
				Math.sin(3.4);
				// First Bayesian prior parameter: E(mu)
				double mu_a_0 = 0;
				for(double m : eventMeans) {
					mu_a_0 += m;
				}
				mu_a_0 /= eventMeans.size();
				
				// Second Bayesian prior parameter: multiplier of precision for mean distribution
				double m_0 = 0;
				for(double v : eventVariances) {
					m_0 += v;
				}
				double m_0_temp = 0;
				for(double m : eventMeans) {
					m_0_temp += (m - mu_a_0) * (m - mu_a_0);
				}
				m_0 /= m_0_temp;
				
				// Third Bayesian prior parameter: shape parameter of gamma distribution for precision
				double meanPrecision = 0;
				double meanLogPrecision = 0;
				double meanPrecisionLogPrecision = 0;
				for(double v : eventVariances) {
					double precision = 1 / v;
					meanPrecision += precision;
					double logPrecision = Math.log(precision);
					meanLogPrecision += logPrecision;
					meanPrecisionLogPrecision += precision * logPrecision;
				}
				meanPrecision /= eventVariances.size();
				meanLogPrecision /= eventVariances.size();
				meanPrecisionLogPrecision /= eventVariances.size();
				double theta_0  = 1/(meanPrecisionLogPrecision - meanPrecision * meanLogPrecision);
				
				// Fourth Bayesian prior parameter: shape parameter of gamma distribution for precision
				double alpha_0 = meanPrecision * theta_0;
				
				// Fifth Bayesian prior: Alpha parameter of beta distribution for DNF rate
				double meanOfDnfRates = 0;
				for(double d : eventDnfRates) {
					meanOfDnfRates += d;
				}
				meanOfDnfRates /= eventDnfRates.size();
				double varOfDnfRates = 0;
				for(double d : eventDnfRates) {
					varOfDnfRates += (d - meanOfDnfRates) * (d - meanOfDnfRates);
				}
				varOfDnfRates /= (eventDnfRates.size() - 1);
				double alep_0 = meanOfDnfRates * (meanOfDnfRates * (1 - meanOfDnfRates) / varOfDnfRates - 1);
				
				// Sixth Bayesian prior: Beta parameter of beta distribution for DNF rate
				double bet_0 = (1 - meanOfDnfRates) * (meanOfDnfRates * (1 - meanOfDnfRates) / varOfDnfRates - 1);
				
				conn.prepareStatement("INSERT INTO cs_bayes_priors (event_id, mu_a_0, m_0, alpha_0, theta_0, alep_0, bet_0) VALUES ('" + event + "', " + mu_a_0 + ", " + m_0 + ", " + alpha_0 + ", " + theta_0 + ", " + alep_0 + ", " + bet_0 + ")").executeUpdate();
			}
			
			result = new TaskResult(true, "Finished calculating Bayesian priors");
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

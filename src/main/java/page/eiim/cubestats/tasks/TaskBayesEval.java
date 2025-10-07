package page.eiim.cubestats.tasks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import page.eiim.cubestats.DatabaseCSN;
import page.eiim.cubestats.DistrMath;
import page.eiim.cubestats.Settings;

public class TaskBayesEval extends Task {
	
	private static final double A = 0.2; // Needs tweaking
	private static final double B = 15; // Needs tweaking
	
	public TaskBayesEval(Settings settings) {
		super(settings);
	}
	
	@Override
	public String name() {
		return "Evaluate Bayesian model";
	}

	@Override
	public void run() {
		try {
			Connection conn = DatabaseCSN.getConnection(settings, DatabaseCSN.DefaultSchema.STAGING);
			
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
			Parameters prior = null;
			Map<String, Double> dnfLLSums = new TreeMap<>();
			Map<String, Double> timeLLSums = new TreeMap<>();
			Map<String, Double> totalLLSums = new TreeMap<>();
			Map<String, Integer> counts = new TreeMap<>();
			while(rs.next()) {
				String person = rs.getString(1);
				String event = rs.getString(2);
				if(!event.equals(currentEvent) || !person.equals(currentPerson)) {
					validEvent = priors.containsKey(event);
					if(validEvent) prior = priors.get(event);
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
					if(time > 0 || time == -1) { // Valid time or DNF
						LogLikelihood ll = getLL(prior, times, dates, dnfs, time);
						dnfLLSums.put(event, dnfLLSums.getOrDefault(event, 0.0) + ll.dnfPart);
						timeLLSums.put(event, timeLLSums.getOrDefault(event, 0.0) + ll.timePart);
						totalLLSums.put(event, totalLLSums.getOrDefault(event, 0.0) + ll.dnfPart + ll.timePart);
						counts.put(event, counts.getOrDefault(event, 0) + 1);
					}
					if(time > 0) {
						times.add(Math.log(time));
						dates.add(date);
					} else if(time == -1) { // DNF
						dnfs++;
					}
				}
			}
			
			System.out.println("Mean log-likelihoods:");
			for(String event : totalLLSums.keySet()) {
				double meanDNF = dnfLLSums.get(event) / counts.get(event);
				double meanTime = timeLLSums.get(event) / counts.get(event);
				double meanTotal = totalLLSums.get(event) / counts.get(event);
				System.out.println(event + ":\tDNF " + meanDNF + ", time " + meanTime + ", total " + meanTotal + " over " + counts.get(event) + " solves");
			}
			double grandDNF = 0;
			double grandTime = 0;
			double grandTotal = 0;
			int grandCount = 0;
			for(String event : totalLLSums.keySet()) {
				grandDNF += dnfLLSums.get(event);
				grandTime += timeLLSums.get(event);
				grandTotal += totalLLSums.get(event);
				grandCount += counts.get(event);
			}
			System.out.println("Overall:\tDNF " + (grandDNF / grandCount) + ", time " + (grandTime / grandCount) + ", total " + (grandTotal / grandCount) + " over " + grandCount + " solves");
			
			result = new TaskResult(true, "Finished evaluating Bayesian model");
			isDone = true;
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			result = new TaskResult(false, e.getMessage());
			isDone = true;
			return;
		}
	}
	
	private static LogLikelihood getLL(Parameters prior, ArrayList<Double> times, ArrayList<LocalDate> dates, int dnfs, int time) {
		double alep = prior.alep + dnfs;
		double bet = prior.bet + times.size();
		if(time == -1) {
			// DNF
			return new LogLikelihood(Math.log(alep/(alep + bet)), 0);
		}
		double pDNF = Math.log(bet/(alep + bet));
		
		// Regular time
		int n = times.size();
		double m_b, mu_a, alpha, theta;
		if(n > 0) {
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
			m_b = sumWeights + prior.m;
			mu_a = (sumWeights/m_b)*weightedMean + (prior.m/m_b)*prior.mu_a;
			alpha = prior.alpha + (sumWeights/2);
			theta = prior.theta + (weightedNVariance/2) + (sumWeights*prior.m)/(2*m_b);
		} else {
			m_b = prior.m;
			mu_a = prior.mu_a;
			alpha = prior.alpha;
			theta = prior.theta;
		}
		
		double logTime = Math.log(time);
		
		double pTime = DistrMath.logGammaDiff(alpha) - 0.5*Math.log(Math.TAU) - 0.5*Math.log(theta) - 0.5*Math.log(1+m_b) - (alpha+0.5)*Math.log(1 + (1/(2*theta*(1+m_b)))*(logTime - mu_a)*(logTime - mu_a));
		return new LogLikelihood(pDNF, pTime);
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
	private static record LogLikelihood(double dnfPart, double timePart) {}
}

package page.eiim.cubestats.tasks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import page.eiim.cubestats.DatabaseCSN;
import page.eiim.cubestats.DistrMath;
import page.eiim.cubestats.Settings;

public class TaskBayesEval extends Task {
	
	private static final double A = 0.05;
	private static final double B = 30;
	private static final double EXP = 0.25;
	private static final Map<String, Parameters> priors = new TreeMap<>();
	private static final List<Result> resultsCache = new ArrayList<>();
	
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
			Connection conn = DatabaseCSN.getConnection(settings, stagingSchema);
			
			if(priors.isEmpty()) {
				ResultSet rsEvents = conn.prepareStatement("SELECT event_id, mu_a_0, m_0, alpha_0, theta_0, alep_0, bet_0 FROM cs_bayes_priors").executeQuery();
				//Map<String, Parameters> priors = new TreeMap<>(); // Is TreeMap best here?
				
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
			}
			
			if(resultsCache.isEmpty()) {
				ResultSet rs = conn.prepareStatement("SELECT results.person_id, results.event_id, competitions.end_date, results.value1, results.value2, results.value3, results.value4, results.value5\r\n"
						+ "FROM results\r\n"
						+ "JOIN competitions ON results.competition_id = competitions.id\r\n"
						+ "ORDER BY person_id, event_id, competitions.end_date ASC").executeQuery();
				while(rs.next()) {
					resultsCache.add(new Result(
							rs.getString(1),
							rs.getString(2),
							rs.getObject(3, LocalDate.class),
							new int[] {
								rs.getInt(4),
								rs.getInt(5),
								rs.getInt(6),
								rs.getInt(7),
								rs.getInt(8)
							}
						)
					);
				}
			}
			
			
			//System.out.println("Getting results...");
			
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
			for(Result r : resultsCache) {
				String person = r.person();
				String event = r.event();
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
				LocalDate date = r.date();
				for(int i = 4; i <= 8; i++) {
					int time = r.times()[i-4];
					if(time > 0 || time == -1) { // Valid time or DNF
						LogLikelihood ll = getLL(prior, times, dates, dnfs, time);
						dnfLLSums.put(event, dnfLLSums.getOrDefault(event, 0.0) + ll.dnfPart);
						timeLLSums.put(event, timeLLSums.getOrDefault(event, 0.0) + ll.timePart);
						totalLLSums.put(event, totalLLSums.getOrDefault(event, 0.0) + ll.dnfPart + ll.timePart);
						counts.put(event, counts.getOrDefault(event, 0) + 1);
					}
					if(time > 0) {
						times.add(Math.log(time+0.5));
						dates.add(date);
					} else if(time == -1) { // DNF
						dnfs++;
					}
				}
			}
			
			System.out.println("Mean log-likelihoods:");
			//System.out.println("event,dnf,time,total,solves");
			for(String event : totalLLSums.keySet()) {
				double meanDNF = dnfLLSums.get(event) / counts.get(event);
				double meanTime = timeLLSums.get(event) / counts.get(event);
				double meanTotal = totalLLSums.get(event) / counts.get(event);
				System.out.println(event + ":\tDNF " + meanDNF + ", time " + meanTime + ", total " + meanTotal + " over " + counts.get(event) + " solves");
				//System.out.println(event + "," + meanDNF + "," + meanTime + "," + meanTotal + "," + counts.get(event));
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
			//System.out.println("Overall," + (grandDNF / grandCount) + "," + (grandTime / grandCount) + "," + (grandTotal / grandCount) + "," + grandCount);
			
			//System.out.println(a + "," + b + "," + exp + "," + useDays + "," + useSolves + "," + (grandTime / grandCount));
			
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
	
	public static LogLikelihood getLL(Parameters prior, List<Double> logtimes, List<LocalDate> dates, int dnfs, int time) {
		double alep = prior.alep + dnfs;
		double bet = prior.bet + logtimes.size();
		if(time == -1) {
			// DNF
			return new LogLikelihood(Math.log(alep/(alep + bet)), 0);
		}
		double pDNF = Math.log(bet/(alep + bet));
		
		// Regular time
		int n = logtimes.size();
		double m_b, mu_a, alpha, theta;
		if(n > 0) {
			double[] weights = calculateWeights(dates);
			double sumWeights = 0;
			for(double w : weights) sumWeights += w;
			double weightedMean = 0;
			for(int i = 0; i < n; i++) {
				weightedMean += weights[i] * logtimes.get(i);
			}
			weightedMean /= sumWeights;
			double weightedNVariance = 0;
			for(int i = 0; i < n; i++) {
				weightedNVariance += weights[i] * (logtimes.get(i) - weightedMean) * (logtimes.get(i) - weightedMean);
			}
			m_b = sumWeights + prior.m;
			mu_a = (sumWeights/m_b)*weightedMean + (prior.m/m_b)*prior.mu_a;
			alpha = prior.alpha + (sumWeights/2);
			theta = prior.theta + (weightedNVariance/2) + (sumWeights*prior.m)/(2*m_b)*(weightedMean-prior.mu_a)*(weightedMean-prior.mu_a);
		} else {
			m_b = prior.m;
			mu_a = prior.mu_a;
			alpha = prior.alpha;
			theta = prior.theta;
		}
		
		double logTime = Math.log(time+0.5);
		
		double pTime = DistrMath.logGammaDiff(alpha) - 0.5*Math.log(Math.TAU) - 0.5*Math.log(theta) - 0.5*Math.log((1+m_b)/m_b) - (alpha+0.5)*Math.log(1 + (m_b/(2*theta*(1+m_b)))*(logTime - mu_a)*(logTime - mu_a));
		return new LogLikelihood(pDNF, pTime);
	}
	
	private static double[] calculateWeights(List<LocalDate> dates) {
		long mostRecent = dates.get(dates.size() - 1).toEpochDay();
		double[] weights = new double[dates.size()];
		double sum = 0;
		double totalWeight = Math.pow(dates.size(), EXP);
		for(int i = 0; i < dates.size(); i++) {
			long days = mostRecent - dates.get(i).toEpochDay();
			int solves = dates.size() - i;
			double weight = 1.0 / (1.0 + Math.exp(A*(days + solves - B)));
			sum += weight;
			weights[i] = weight;
		}
		// Normalize weights
		sum /= totalWeight;
		for(int i = 0; i < weights.length; i++) {
			weights[i] /= sum;
		}
		
		return weights;
	}
	
	public static record Parameters(double mu_a, double m, double alpha, double theta, double alep, double bet) {}
	public static record LogLikelihood(double dnfPart, double timePart) {}
	
	public static void main(String[] args) {
		Settings.Builder sb = new Settings.Builder();
		Settings settings = sb.build();
		Task task;
		if(false) {
			task = new TaskBayesPriors(settings);
			System.out.println("Calculating priors...");
			task.run();
			System.out.println("Priors complete");
		}
		if(true) {
			TaskBayesEval evalTask = new TaskBayesEval(settings);
			evalTask.run();
		}
	}
	
	private static record Result(String person, String event, LocalDate date, int[] times) {};
}

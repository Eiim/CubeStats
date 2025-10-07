package page.eiim.cubestats.tasks;

import java.sql.Connection;
import java.sql.SQLException;

import page.eiim.cubestats.DatabaseCSN;
import page.eiim.cubestats.Settings;

public class TaskPercentiles extends Task {
	
	public TaskPercentiles(Settings settings) {
		super(settings);
	}
	
	@Override
	public String name() {
		return "Generate Percentiles";
	}

	@Override
	public void run() {
		try {
			Connection conn = DatabaseCSN.getConnection(settings, DatabaseCSN.DefaultSchema.STAGING);
			
			conn.prepareStatement("DROP TABLE IF EXISTS cs_pct_single").executeUpdate();
			conn.prepareStatement("CREATE TABLE cs_pct_single AS (" + """
					WITH ranked AS (
						SELECT
							event_id,
							best,
							ROW_NUMBER() OVER (PARTITION BY event_id ORDER BY best) AS rn,
							COUNT(*) OVER (PARTITION BY event_id) AS cnt
						FROM ranks_single
					)
					SELECT
						event_id,
						MIN(CASE WHEN rn = 1 THEN best END) AS min,
						MIN(CASE WHEN rn = FLOOR(0.01*cnt)+1 THEN best END) AS p01,
						MIN(CASE WHEN rn = FLOOR(0.02*cnt)+1 THEN best END) AS p02,
						MIN(CASE WHEN rn = FLOOR(0.03*cnt)+1 THEN best END) AS p03,
						MIN(CASE WHEN rn = FLOOR(0.04*cnt)+1 THEN best END) AS p04,
						MIN(CASE WHEN rn = FLOOR(0.05*cnt)+1 THEN best END) AS p05,
						MIN(CASE WHEN rn = FLOOR(0.06*cnt)+1 THEN best END) AS p06,
						MIN(CASE WHEN rn = FLOOR(0.07*cnt)+1 THEN best END) AS p07,
						MIN(CASE WHEN rn = FLOOR(0.08*cnt)+1 THEN best END) AS p08,
						MIN(CASE WHEN rn = FLOOR(0.09*cnt)+1 THEN best END) AS p09,
						MIN(CASE WHEN rn = FLOOR(0.10*cnt)+1 THEN best END) AS p10,
						MIN(CASE WHEN rn = FLOOR(0.11*cnt)+1 THEN best END) AS p11,
						MIN(CASE WHEN rn = FLOOR(0.12*cnt)+1 THEN best END) AS p12,
						MIN(CASE WHEN rn = FLOOR(0.13*cnt)+1 THEN best END) AS p13,
						MIN(CASE WHEN rn = FLOOR(0.14*cnt)+1 THEN best END) AS p14,
						MIN(CASE WHEN rn = FLOOR(0.15*cnt)+1 THEN best END) AS p15,
						MIN(CASE WHEN rn = FLOOR(0.16*cnt)+1 THEN best END) AS p16,
						MIN(CASE WHEN rn = FLOOR(0.17*cnt)+1 THEN best END) AS p17,
						MIN(CASE WHEN rn = FLOOR(0.18*cnt)+1 THEN best END) AS p18,
						MIN(CASE WHEN rn = FLOOR(0.19*cnt)+1 THEN best END) AS p19,
						MIN(CASE WHEN rn = FLOOR(0.20*cnt)+1 THEN best END) AS p20,
						MIN(CASE WHEN rn = FLOOR(0.21*cnt)+1 THEN best END) AS p21,
						MIN(CASE WHEN rn = FLOOR(0.22*cnt)+1 THEN best END) AS p22,
						MIN(CASE WHEN rn = FLOOR(0.23*cnt)+1 THEN best END) AS p23,
						MIN(CASE WHEN rn = FLOOR(0.24*cnt)+1 THEN best END) AS p24,
						MIN(CASE WHEN rn = FLOOR(0.25*cnt)+1 THEN best END) AS p25,
						MIN(CASE WHEN rn = FLOOR(0.26*cnt)+1 THEN best END) AS p26,
						MIN(CASE WHEN rn = FLOOR(0.27*cnt)+1 THEN best END) AS p27,
						MIN(CASE WHEN rn = FLOOR(0.28*cnt)+1 THEN best END) AS p28,
						MIN(CASE WHEN rn = FLOOR(0.29*cnt)+1 THEN best END) AS p29,
						MIN(CASE WHEN rn = FLOOR(0.30*cnt)+1 THEN best END) AS p30,
						MIN(CASE WHEN rn = FLOOR(0.31*cnt)+1 THEN best END) AS p31,
						MIN(CASE WHEN rn = FLOOR(0.32*cnt)+1 THEN best END) AS p32,
						MIN(CASE WHEN rn = FLOOR(0.33*cnt)+1 THEN best END) AS p33,
						MIN(CASE WHEN rn = FLOOR(0.34*cnt)+1 THEN best END) AS p34,
						MIN(CASE WHEN rn = FLOOR(0.35*cnt)+1 THEN best END) AS p35,
						MIN(CASE WHEN rn = FLOOR(0.36*cnt)+1 THEN best END) AS p36,
						MIN(CASE WHEN rn = FLOOR(0.37*cnt)+1 THEN best END) AS p37,
						MIN(CASE WHEN rn = FLOOR(0.38*cnt)+1 THEN best END) AS p38,
						MIN(CASE WHEN rn = FLOOR(0.39*cnt)+1 THEN best END) AS p39,
						MIN(CASE WHEN rn = FLOOR(0.40*cnt)+1 THEN best END) AS p40,
						MIN(CASE WHEN rn = FLOOR(0.41*cnt)+1 THEN best END) AS p41,
						MIN(CASE WHEN rn = FLOOR(0.42*cnt)+1 THEN best END) AS p42,
						MIN(CASE WHEN rn = FLOOR(0.43*cnt)+1 THEN best END) AS p43,
						MIN(CASE WHEN rn = FLOOR(0.44*cnt)+1 THEN best END) AS p44,
						MIN(CASE WHEN rn = FLOOR(0.45*cnt)+1 THEN best END) AS p45,
						MIN(CASE WHEN rn = FLOOR(0.46*cnt)+1 THEN best END) AS p46,
						MIN(CASE WHEN rn = FLOOR(0.47*cnt)+1 THEN best END) AS p47,
						MIN(CASE WHEN rn = FLOOR(0.48*cnt)+1 THEN best END) AS p48,
						MIN(CASE WHEN rn = FLOOR(0.49*cnt)+1 THEN best END) AS p49,
						MIN(CASE WHEN rn = FLOOR(0.50*cnt)+1 THEN best END) AS p50,
						MIN(CASE WHEN rn = FLOOR(0.51*cnt)+1 THEN best END) AS p51,
						MIN(CASE WHEN rn = FLOOR(0.52*cnt)+1 THEN best END) AS p52,
						MIN(CASE WHEN rn = FLOOR(0.53*cnt)+1 THEN best END) AS p53,
						MIN(CASE WHEN rn = FLOOR(0.54*cnt)+1 THEN best END) AS p54,
						MIN(CASE WHEN rn = FLOOR(0.55*cnt)+1 THEN best END) AS p55,
						MIN(CASE WHEN rn = FLOOR(0.56*cnt)+1 THEN best END) AS p56,
						MIN(CASE WHEN rn = FLOOR(0.57*cnt)+1 THEN best END) AS p57,
						MIN(CASE WHEN rn = FLOOR(0.58*cnt)+1 THEN best END) AS p58,
						MIN(CASE WHEN rn = FLOOR(0.59*cnt)+1 THEN best END) AS p59,
						MIN(CASE WHEN rn = FLOOR(0.60*cnt)+1 THEN best END) AS p60,
						MIN(CASE WHEN rn = FLOOR(0.61*cnt)+1 THEN best END) AS p61,
						MIN(CASE WHEN rn = FLOOR(0.62*cnt)+1 THEN best END) AS p62,
						MIN(CASE WHEN rn = FLOOR(0.63*cnt)+1 THEN best END) AS p63,
						MIN(CASE WHEN rn = FLOOR(0.64*cnt)+1 THEN best END) AS p64,
						MIN(CASE WHEN rn = FLOOR(0.65*cnt)+1 THEN best END) AS p65,
						MIN(CASE WHEN rn = FLOOR(0.66*cnt)+1 THEN best END) AS p66,
						MIN(CASE WHEN rn = FLOOR(0.67*cnt)+1 THEN best END) AS p67,
						MIN(CASE WHEN rn = FLOOR(0.68*cnt)+1 THEN best END) AS p68,
						MIN(CASE WHEN rn = FLOOR(0.69*cnt)+1 THEN best END) AS p69,
						MIN(CASE WHEN rn = FLOOR(0.70*cnt)+1 THEN best END) AS p70,
						MIN(CASE WHEN rn = FLOOR(0.71*cnt)+1 THEN best END) AS p71,
						MIN(CASE WHEN rn = FLOOR(0.72*cnt)+1 THEN best END) AS p72,
						MIN(CASE WHEN rn = FLOOR(0.73*cnt)+1 THEN best END) AS p73,
						MIN(CASE WHEN rn = FLOOR(0.74*cnt)+1 THEN best END) AS p74,
						MIN(CASE WHEN rn = FLOOR(0.75*cnt)+1 THEN best END) AS p75,
						MIN(CASE WHEN rn = FLOOR(0.76*cnt)+1 THEN best END) AS p76,
						MIN(CASE WHEN rn = FLOOR(0.77*cnt)+1 THEN best END) AS p77,
						MIN(CASE WHEN rn = FLOOR(0.78*cnt)+1 THEN best END) AS p78,
						MIN(CASE WHEN rn = FLOOR(0.79*cnt)+1 THEN best END) AS p79,
						MIN(CASE WHEN rn = FLOOR(0.80*cnt)+1 THEN best END) AS p80,
						MIN(CASE WHEN rn = FLOOR(0.81*cnt)+1 THEN best END) AS p81,
						MIN(CASE WHEN rn = FLOOR(0.82*cnt)+1 THEN best END) AS p82,
						MIN(CASE WHEN rn = FLOOR(0.83*cnt)+1 THEN best END) AS p83,
						MIN(CASE WHEN rn = FLOOR(0.84*cnt)+1 THEN best END) AS p84,
						MIN(CASE WHEN rn = FLOOR(0.85*cnt)+1 THEN best END) AS p85,
						MIN(CASE WHEN rn = FLOOR(0.86*cnt)+1 THEN best END) AS p86,
						MIN(CASE WHEN rn = FLOOR(0.87*cnt)+1 THEN best END) AS p87,
						MIN(CASE WHEN rn = FLOOR(0.88*cnt)+1 THEN best END) AS p88,
						MIN(CASE WHEN rn = FLOOR(0.89*cnt)+1 THEN best END) AS p89,
						MIN(CASE WHEN rn = FLOOR(0.90*cnt)+1 THEN best END) AS p90,
						MIN(CASE WHEN rn = FLOOR(0.91*cnt)+1 THEN best END) AS p91,
						MIN(CASE WHEN rn = FLOOR(0.92*cnt)+1 THEN best END) AS p92,
						MIN(CASE WHEN rn = FLOOR(0.93*cnt)+1 THEN best END) AS p93,
						MIN(CASE WHEN rn = FLOOR(0.94*cnt)+1 THEN best END) AS p94,
						MIN(CASE WHEN rn = FLOOR(0.95*cnt)+1 THEN best END) AS p95,
						MIN(CASE WHEN rn = FLOOR(0.96*cnt)+1 THEN best END) AS p96,
						MIN(CASE WHEN rn = FLOOR(0.97*cnt)+1 THEN best END) AS p97,
						MIN(CASE WHEN rn = FLOOR(0.98*cnt)+1 THEN best END) AS p98,
						MIN(CASE WHEN rn = FLOOR(0.99*cnt)+1 THEN best END) AS p99,
						MIN(CASE WHEN rn = cnt THEN best END) AS max
					FROM ranked
					GROUP BY event_id
					);
					""").executeUpdate();
			
			conn.prepareStatement("DROP TABLE IF EXISTS cs_pct_average").executeUpdate();
			conn.prepareStatement("CREATE TABLE cs_pct_average AS (" + """
					WITH ranked AS (
						SELECT
							event_id,
							best,
							ROW_NUMBER() OVER (PARTITION BY event_id ORDER BY best) AS rn,
							COUNT(*) OVER (PARTITION BY event_id) AS cnt
						FROM ranks_average
					)
					SELECT
						event_id,
						MIN(CASE WHEN rn = 1 THEN best END) AS min,
						MIN(CASE WHEN rn = FLOOR(0.01*cnt)+1 THEN best END) AS p01,
						MIN(CASE WHEN rn = FLOOR(0.02*cnt)+1 THEN best END) AS p02,
						MIN(CASE WHEN rn = FLOOR(0.03*cnt)+1 THEN best END) AS p03,
						MIN(CASE WHEN rn = FLOOR(0.04*cnt)+1 THEN best END) AS p04,
						MIN(CASE WHEN rn = FLOOR(0.05*cnt)+1 THEN best END) AS p05,
						MIN(CASE WHEN rn = FLOOR(0.06*cnt)+1 THEN best END) AS p06,
						MIN(CASE WHEN rn = FLOOR(0.07*cnt)+1 THEN best END) AS p07,
						MIN(CASE WHEN rn = FLOOR(0.08*cnt)+1 THEN best END) AS p08,
						MIN(CASE WHEN rn = FLOOR(0.09*cnt)+1 THEN best END) AS p09,
						MIN(CASE WHEN rn = FLOOR(0.10*cnt)+1 THEN best END) AS p10,
						MIN(CASE WHEN rn = FLOOR(0.11*cnt)+1 THEN best END) AS p11,
						MIN(CASE WHEN rn = FLOOR(0.12*cnt)+1 THEN best END) AS p12,
						MIN(CASE WHEN rn = FLOOR(0.13*cnt)+1 THEN best END) AS p13,
						MIN(CASE WHEN rn = FLOOR(0.14*cnt)+1 THEN best END) AS p14,
						MIN(CASE WHEN rn = FLOOR(0.15*cnt)+1 THEN best END) AS p15,
						MIN(CASE WHEN rn = FLOOR(0.16*cnt)+1 THEN best END) AS p16,
						MIN(CASE WHEN rn = FLOOR(0.17*cnt)+1 THEN best END) AS p17,
						MIN(CASE WHEN rn = FLOOR(0.18*cnt)+1 THEN best END) AS p18,
						MIN(CASE WHEN rn = FLOOR(0.19*cnt)+1 THEN best END) AS p19,
						MIN(CASE WHEN rn = FLOOR(0.20*cnt)+1 THEN best END) AS p20,
						MIN(CASE WHEN rn = FLOOR(0.21*cnt)+1 THEN best END) AS p21,
						MIN(CASE WHEN rn = FLOOR(0.22*cnt)+1 THEN best END) AS p22,
						MIN(CASE WHEN rn = FLOOR(0.23*cnt)+1 THEN best END) AS p23,
						MIN(CASE WHEN rn = FLOOR(0.24*cnt)+1 THEN best END) AS p24,
						MIN(CASE WHEN rn = FLOOR(0.25*cnt)+1 THEN best END) AS p25,
						MIN(CASE WHEN rn = FLOOR(0.26*cnt)+1 THEN best END) AS p26,
						MIN(CASE WHEN rn = FLOOR(0.27*cnt)+1 THEN best END) AS p27,
						MIN(CASE WHEN rn = FLOOR(0.28*cnt)+1 THEN best END) AS p28,
						MIN(CASE WHEN rn = FLOOR(0.29*cnt)+1 THEN best END) AS p29,
						MIN(CASE WHEN rn = FLOOR(0.30*cnt)+1 THEN best END) AS p30,
						MIN(CASE WHEN rn = FLOOR(0.31*cnt)+1 THEN best END) AS p31,
						MIN(CASE WHEN rn = FLOOR(0.32*cnt)+1 THEN best END) AS p32,
						MIN(CASE WHEN rn = FLOOR(0.33*cnt)+1 THEN best END) AS p33,
						MIN(CASE WHEN rn = FLOOR(0.34*cnt)+1 THEN best END) AS p34,
						MIN(CASE WHEN rn = FLOOR(0.35*cnt)+1 THEN best END) AS p35,
						MIN(CASE WHEN rn = FLOOR(0.36*cnt)+1 THEN best END) AS p36,
						MIN(CASE WHEN rn = FLOOR(0.37*cnt)+1 THEN best END) AS p37,
						MIN(CASE WHEN rn = FLOOR(0.38*cnt)+1 THEN best END) AS p38,
						MIN(CASE WHEN rn = FLOOR(0.39*cnt)+1 THEN best END) AS p39,
						MIN(CASE WHEN rn = FLOOR(0.40*cnt)+1 THEN best END) AS p40,
						MIN(CASE WHEN rn = FLOOR(0.41*cnt)+1 THEN best END) AS p41,
						MIN(CASE WHEN rn = FLOOR(0.42*cnt)+1 THEN best END) AS p42,
						MIN(CASE WHEN rn = FLOOR(0.43*cnt)+1 THEN best END) AS p43,
						MIN(CASE WHEN rn = FLOOR(0.44*cnt)+1 THEN best END) AS p44,
						MIN(CASE WHEN rn = FLOOR(0.45*cnt)+1 THEN best END) AS p45,
						MIN(CASE WHEN rn = FLOOR(0.46*cnt)+1 THEN best END) AS p46,
						MIN(CASE WHEN rn = FLOOR(0.47*cnt)+1 THEN best END) AS p47,
						MIN(CASE WHEN rn = FLOOR(0.48*cnt)+1 THEN best END) AS p48,
						MIN(CASE WHEN rn = FLOOR(0.49*cnt)+1 THEN best END) AS p49,
						MIN(CASE WHEN rn = FLOOR(0.50*cnt)+1 THEN best END) AS p50,
						MIN(CASE WHEN rn = FLOOR(0.51*cnt)+1 THEN best END) AS p51,
						MIN(CASE WHEN rn = FLOOR(0.52*cnt)+1 THEN best END) AS p52,
						MIN(CASE WHEN rn = FLOOR(0.53*cnt)+1 THEN best END) AS p53,
						MIN(CASE WHEN rn = FLOOR(0.54*cnt)+1 THEN best END) AS p54,
						MIN(CASE WHEN rn = FLOOR(0.55*cnt)+1 THEN best END) AS p55,
						MIN(CASE WHEN rn = FLOOR(0.56*cnt)+1 THEN best END) AS p56,
						MIN(CASE WHEN rn = FLOOR(0.57*cnt)+1 THEN best END) AS p57,
						MIN(CASE WHEN rn = FLOOR(0.58*cnt)+1 THEN best END) AS p58,
						MIN(CASE WHEN rn = FLOOR(0.59*cnt)+1 THEN best END) AS p59,
						MIN(CASE WHEN rn = FLOOR(0.60*cnt)+1 THEN best END) AS p60,
						MIN(CASE WHEN rn = FLOOR(0.61*cnt)+1 THEN best END) AS p61,
						MIN(CASE WHEN rn = FLOOR(0.62*cnt)+1 THEN best END) AS p62,
						MIN(CASE WHEN rn = FLOOR(0.63*cnt)+1 THEN best END) AS p63,
						MIN(CASE WHEN rn = FLOOR(0.64*cnt)+1 THEN best END) AS p64,
						MIN(CASE WHEN rn = FLOOR(0.65*cnt)+1 THEN best END) AS p65,
						MIN(CASE WHEN rn = FLOOR(0.66*cnt)+1 THEN best END) AS p66,
						MIN(CASE WHEN rn = FLOOR(0.67*cnt)+1 THEN best END) AS p67,
						MIN(CASE WHEN rn = FLOOR(0.68*cnt)+1 THEN best END) AS p68,
						MIN(CASE WHEN rn = FLOOR(0.69*cnt)+1 THEN best END) AS p69,
						MIN(CASE WHEN rn = FLOOR(0.70*cnt)+1 THEN best END) AS p70,
						MIN(CASE WHEN rn = FLOOR(0.71*cnt)+1 THEN best END) AS p71,
						MIN(CASE WHEN rn = FLOOR(0.72*cnt)+1 THEN best END) AS p72,
						MIN(CASE WHEN rn = FLOOR(0.73*cnt)+1 THEN best END) AS p73,
						MIN(CASE WHEN rn = FLOOR(0.74*cnt)+1 THEN best END) AS p74,
						MIN(CASE WHEN rn = FLOOR(0.75*cnt)+1 THEN best END) AS p75,
						MIN(CASE WHEN rn = FLOOR(0.76*cnt)+1 THEN best END) AS p76,
						MIN(CASE WHEN rn = FLOOR(0.77*cnt)+1 THEN best END) AS p77,
						MIN(CASE WHEN rn = FLOOR(0.78*cnt)+1 THEN best END) AS p78,
						MIN(CASE WHEN rn = FLOOR(0.79*cnt)+1 THEN best END) AS p79,
						MIN(CASE WHEN rn = FLOOR(0.80*cnt)+1 THEN best END) AS p80,
						MIN(CASE WHEN rn = FLOOR(0.81*cnt)+1 THEN best END) AS p81,
						MIN(CASE WHEN rn = FLOOR(0.82*cnt)+1 THEN best END) AS p82,
						MIN(CASE WHEN rn = FLOOR(0.83*cnt)+1 THEN best END) AS p83,
						MIN(CASE WHEN rn = FLOOR(0.84*cnt)+1 THEN best END) AS p84,
						MIN(CASE WHEN rn = FLOOR(0.85*cnt)+1 THEN best END) AS p85,
						MIN(CASE WHEN rn = FLOOR(0.86*cnt)+1 THEN best END) AS p86,
						MIN(CASE WHEN rn = FLOOR(0.87*cnt)+1 THEN best END) AS p87,
						MIN(CASE WHEN rn = FLOOR(0.88*cnt)+1 THEN best END) AS p88,
						MIN(CASE WHEN rn = FLOOR(0.89*cnt)+1 THEN best END) AS p89,
						MIN(CASE WHEN rn = FLOOR(0.90*cnt)+1 THEN best END) AS p90,
						MIN(CASE WHEN rn = FLOOR(0.91*cnt)+1 THEN best END) AS p91,
						MIN(CASE WHEN rn = FLOOR(0.92*cnt)+1 THEN best END) AS p92,
						MIN(CASE WHEN rn = FLOOR(0.93*cnt)+1 THEN best END) AS p93,
						MIN(CASE WHEN rn = FLOOR(0.94*cnt)+1 THEN best END) AS p94,
						MIN(CASE WHEN rn = FLOOR(0.95*cnt)+1 THEN best END) AS p95,
						MIN(CASE WHEN rn = FLOOR(0.96*cnt)+1 THEN best END) AS p96,
						MIN(CASE WHEN rn = FLOOR(0.97*cnt)+1 THEN best END) AS p97,
						MIN(CASE WHEN rn = FLOOR(0.98*cnt)+1 THEN best END) AS p98,
						MIN(CASE WHEN rn = FLOOR(0.99*cnt)+1 THEN best END) AS p99,
						MIN(CASE WHEN rn = cnt THEN best END) AS max
					FROM ranked
					GROUP BY event_id
					);
					""").executeUpdate();
			
			result = new TaskResult(true, "Finished generating percentiles");
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

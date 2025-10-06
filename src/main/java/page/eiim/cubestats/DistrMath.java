package page.eiim.cubestats;

import java.util.Arrays;

public final class DistrMath {
	private static final double HALFLN2PI = 0.9189385332046728;
	private static final double MAGIC_1 = 0.08106146679532726;
	private static final double EULER_MASCHERONI = 0.5772156649015329;
	
	// Pretty rough approximation, but hopefully fast.
	public static double logGamma(double x) {
		if (x > .87) {
			return (x - .5) * Math.log(x) - x + HALFLN2PI + (MAGIC_1 / x);
		} else if (x > .02) {
			return -1.54599 + Math.exp(0.4355-x) + (0.975377*x) - Math.log(x);
		} else {
			return -EULER_MASCHERONI - Math.log(x);
		}
	}
	
	// Custom-tuned to minimize worst-case absolute error (~.000026 @ x ~= 2.33)
	// Note that this does not give exact zeroes at x=1 and x=2.
	public static double logGamma2(double x) {
		double xp = x + 1;
		return (x + .5)*Math.log(xp) - xp + HALFLN2PI + (1/(12.02*xp)) - 1/(464*xp*xp*xp) - Math.log(x);
	}
	
	// logGamma(x+.5) - logGamma(x)
	// Faster and lower error for x >= 1 (worst-case absolute error ~-.000018 @ x ~= 1.193)
	// Should almost never need to be used for x < 1
	public static double logGammaDiff(double x) {
		if(x >= 1) {
			Math.exp(1.0);
			return 0.5 * Math.log(x) - 0.125/x + 0.00422681*Math.pow(x, -2.762); // Pow is expensive, consider alternatives
		} else { // Haven't found a better approximation for x < 1 yet, and it's probably not needed
			return logGamma(x + 0.5) - logGamma(x);
		}
	}
	
	public static double pdfGamma(double x, double scale, double cons) {
		if (x == 0.0) {
			return 0.0;
		}
		double cxs = cons * x / scale;
		double logVal = cons * Math.log(cxs) - cxs - logGamma(cons);
		return Math.exp(logVal)/x;
	}
	
	public static double cdfGammaPoint(double t, double cons) {
		return Math.exp((cons-1) * Math.log(t) - t - logGamma(cons));
	}
	
	public static double logLowerGamma(double s, double x, double iter) {
		double coef = s * Math.log(x) - x;
		double sum = 0;
		for (int i = 0; i < iter; i++) {
			double poch = 0.0;
			for (int j = 0; j < i; j++) {
				poch += Math.log(s + j);
			}
            sum += Math.exp(i * Math.log(x) - poch);
        }
		return coef + Math.log(sum);
	}
	
	public static double cdfGamma2(double x, double scale, double cons, int iter) {
		if (x <= 0.0) {
			return 0.0;
		}
		return Math.exp(logLowerGamma(cons, x * cons / scale, iter) - logGamma(cons));
	}
	
	public static double cdfGamma3(double x, double scale, double cons, int iter) {
		if (x <= 0.0) {
			return 0.0;
		} else if (x > 0.5) {
			return cdfGamma(x, scale, cons, iter); // probably won't need?
		}
		double sum = -x/(1+cons);
		if(x > .001) {
			sum += x*x/(2*(cons+2));
			if(x > .01)  {
				sum -= x*x*x/(6*(cons+3)); // We should never need more iterations than this hopefully
			}
		}
		return sum;
	}
	
	public static double cdfGamma(double x, double scale, double cons, int points) {
		double[] yvals = new double[points];
		double delta = (x * cons / scale)/(points - 1);
		double xtest = 0.0;
		for (int i = 0; i < points; i++) {
			xtest = i * delta;
			yvals[i] = cdfGammaPoint(xtest, cons);
		}
		// Use Simpson's rule for numerical integration
		double sum = 0.0;
		for (int i = 0; i < points - 1; i++) {
			if (i == 0 || i == points - 2) {
				sum += yvals[i];
			} else if (i % 2 == 0) {
				sum += 2 * yvals[i];
			} else {
				sum += 4 * yvals[i];
			}
		}
		sum *= delta / 3.0;
		return Math.min(1.0, sum);
	}
	
	public static double pdfConvGamma(int x, double scale, double cons, int points1, int points2) {
		double[] yvals = new double[points1];
		double delta = 1.0/(points1-1);
		double xtest = 0.0;
		for (int i = 0; i < points1; i++) {
			xtest = i * delta;
			//System.out.println("xtest: " + xtest + ", x*scale: " + x * scale + ", x*cons: " + x * cons);
			yvals[i] = (1-cdfGamma2(1 - xtest, scale, cons, points2))*pdfGamma(xtest, x*scale, x*cons);
		}
		System.out.println(Arrays.toString(yvals));
		// Use Simpson's rule for numerical integration
		double sum = 0.0;
		for (int i = 0; i < points1 - 1; i++) {
			if (i == 0 || i == points1 - 2) {
				sum += yvals[i];
			} else if (i % 2 == 0) {
				sum += 2 * yvals[i];
			} else {
				sum += 4 * yvals[i];
			}
		}
		sum *= delta / 3.0;
		return sum;
	}
}

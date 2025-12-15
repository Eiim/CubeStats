// Utilities for calculating the CDF of the t-distribution
// Adapted from Numerical Recipes, Third Edition
const METHOD_SWITCH = 3000;
const FPMIN = Number.MIN_VALUE / Number.EPSILON;
const CF_ITER = 10000; // Maximum number of iterations, will often be lower
const GAM_COF = [57.1562356658629235, -59.5979603554754912, 14.1360979747417471, -0.491913816097620199, .339946499848118887e-4, .465236289270485756e-4, -.983744753048795646e-4, .158088703224912494e-3, -.210264441724104883e-3, .217439618115212643e-3,-.164318106536763890e-3, .844182239838527433e-4,-.261908384015814087e-4,.368991826595316234e-5];
const QUAD_Y = [0.0021695375159141994, 0.011413521097787704, 0.027972308950302116, 0.051727015600492421, 0.082502225484340941, 0.12007019910960293, 0.16415283300752470, 0.21442376986779355, 0.27051082840644336, 0.33199876341447887, 0.39843234186401943, 0.46931971407375483, 0.54413605556657973, 0.62232745288031077, 0.70331500465597174, 0.78649910768313447, 0.87126389619061517, 0.95698180152629142];
const QUAD_W = [0.0055657196642445571, 0.012915947284065419, 0.020181515297735382, 0.027298621498568734, 0.034213810770299537,0.040875750923643261,0.047235083490265582,0.053244713977759692,0.058860144245324798,0.064039797355015485,0.068745323835736408,0.072941885005653087,0.076598410645870640,0.079687828912071670,0.082187266704339706,0.084078218979661945,0.085346685739338721,0.085983275670394821];

// location-scale t-distribution CDF
function tlscdf(df, mu, tau, x) {
	return tcdf(df, (x-mu)/tau);
}

// estimated inverse location-scale t-distribution CDF
// seems to break down around .0002, which is good enough for now
function tlsicdfe(df, mu, tau, p) {
	// Super rough tan-based estimate
	let xhat2 = Math.tan(Math.PI*(p-.5));
	xhat2 = Math.sign(xhat2) * Math.pow(Math.abs(xhat2), .2);
	let phat2 = tcdf(df, xhat2);
	// Iterative approach
	let xhat = (phat2 > p) ? xhat2/2 : xhat2*2;
	let phat = tcdf(df, xhat)
	while(Math.abs(phat-phat2) > .00001) {
		let xhatnew = xhat + (xhat2-xhat)*((p-phat)/(phat2-phat));
		let phatnew = tcdf(df, xhatnew);
		xhat2 = xhat;
		phat2 = phat;
		xhat = xhatnew;
		phat = phatnew;
	}
	return tau*xhat + mu;
}

// t-distribution CDF
function tcdf(v, x) {
	let res = betai(v/2, 0.5, v/(x*x+v))/2;
	return x > 0 ? 1 - res : res;
}

// There aren't a lot of optimizations to be had by hardcoding b=1/2, but maybe interesting to look at
function betai(a, b, x) {
	if(a > METHOD_SWITCH && b > METHOD_SWITCH) {
		return betaiquad(a, b, x);
	}
	let bt = Math.exp(gammaln(a+b)-gammaln(a)-gammaln(b)+a*Math.log(x)+b*Math.log(1-x));
	if(x < (a+1)/(a+b+2)) {
		return bt*betaicf(a, b, x)/a;
	} else {
		return 1-(bt*betaicf(b, a, 1-x)/b);
	}
}

// ln(Gamma(x))
function gammaln(xx) {
	 let y = xx;
	 let x = xx;
	 let tmp = x + 5.24218750000000000;
	 tmp = (x+.5)*Math.log(tmp)-tmp;
	 let ser = 0.999999999999997092;
	 for (let j = 0; j < 14; j++) {
		 ser += GAM_COF[j]/++y;
	 }
	 return tmp + Math.log(2.5066282746310005*ser/x);
}

function betaicf(a, b, x) {
	let qab = a+b;
	let qap = a+1;
	let qam = a-1;
	let c = 1;
	let d = 1 - qab*x/qap;
	if(Math.abs(d) < FPMIN) {
		d = FPMIN; // Should never be negative due to symmetry check?
	}
	d = 1/d;
	let h = d;
	for(let m = 1; m < CF_ITER; m++) {
		let m2 = m*2;
		let aa = m*(b-m)*x/((qam+m2)*(a+m2));
		d = 1 + aa*d;
		if (Math.abs(d) < FPMIN) {d=FPMIN};
		c = 1 + aa/c;
		if (Math.abs(c) < FPMIN) {c=FPMIN};
		d = 1/d;
		h *= d*c;
		aa = -(a+m)*(qab+m)*x/((a+m2)*(qap+m2));
		d = 1 + aa*d;
		if (Math.abs(d) < FPMIN) {d=FPMIN};
		c = 1 + aa/c;
		if (Math.abs(c) < FPMIN) {c=FPMIN};
		d = 1/d;
		del = d*c;
		h *= del;
		if (Math.abs(del-1.0) <= Number.EPSILON) break;
	}
	return h;
}

// Should never encounter numbers large enough to justify this, but it's nice to have in case
function betaiquad(a, b, x) {
	let a1 = a-1;
	let b1 = b-1;
	let mu = a/(a+b);
	let lnmu = Math.log(mu);
	let lnmuc = Math.log1p(-mu); // ln(1-mu), slightly more numerically stable
	let t = Math.sqrt(a*b/((a+b)*(a+b)*(a+b+1)));
	let xu = 0;
	if(x > a/(a+b)) {
		if(x >= 1) {return 1;}
		xu = Math.min(1, Math.max(mu + 10*t, x + 5*t));
	} else {
		if(x <= 0) {return 0;}
		xu = Math.max(0, Math.min(mu - 10*t, x - 5*t));
	}
	let sum = 0;
	for(let j = 0; j < 18; j++) {
		t = x + (xu-x)*QUAD_Y[j];
		sum += QUAD_W[j]*Math.exp(a1*(Math.log(t)-lnmu) + b1*(Math.log(1-t)-lnmuc));
	}
	let ans = sum*(xu-x)*Math.exp(a1*lnmu - gammaln(a) + b1*lnmuc-gammaln(b) + gammaln(a+b));
	return ans > 0 ? 1-ans : -ans;
}
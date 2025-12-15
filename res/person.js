let modeCDF = false;
needsRefresh = false;

events = Object.keys(params);
events.splice(events.indexOf(curEvent), 1);
events.unshift(curEvent);
eventData = {};
curEventData = {};

for(calcEvent of events) {
	let bayes = params[calcEvent];
	
	// Time calculations
	let df = 2*bayes.alpha;
	let tau = Math.sqrt((1+bayes.m)*bayes.theta/(bayes.m*bayes.alpha))
	let leftbound = tlsicdfe(2*bayes.alpha, bayes.mu, tau, .001);
	let rightbound = tlsicdfe(2*bayes.alpha, bayes.mu, tau, .999);
	leftbound = Math.floor(Math.exp(leftbound));
	rightbound = Math.ceil(Math.exp(rightbound));
	let points = Math.min(rightbound-leftbound, 1000);

	let pdf = [];
	let cdf = [];
	let maxPdf = 0;
	for(i = 0; i < points; i++) {
		let lnx = Math.log(leftbound + (i/points)*(rightbound-leftbound));
		let lnxm = Math.log(leftbound + (i/points)*(rightbound-leftbound) - 1);
		cdf[i] = tlscdf(df, bayes.mu, tau, lnx);
		pdf[i] = cdf[i] - tlscdf(df, bayes.mu, tau, lnxm);
		if(pdf[i] > maxPdf) {maxPdf = pdf[i]}
	}
	maxPdf *= 1.01;

	let pdfPoints = "";
	let cdfPoints = "";
	for(i = 0; i < points; i++) {
		// Adding a decimal place on X helps a bit with small ranges
		xPct = (1000*i/(points-1)).toFixed(1);
		// Two decimal places on the Y makes for very slightly smoother curves at worse memory cost
		yPctP = (600-(600*pdf[i]/maxPdf)).toFixed(1);
		yPctC = (600-(600*cdf[i])).toFixed(1);
		pdfPoints += xPct+","+yPctP+" ";
		cdfPoints += xPct+","+yPctC+" ";
	}
	
	eventData[calcEvent] = {
		leftbound: leftbound,
		rightbound: rightbound,
		points: points,
		pdf: pdf,
		cdf: cdf,
		maxPdf: maxPdf,
		pdfPoints: pdfPoints,
		cdfPoints: cdfPoints
	};
	
	if(calcEvent == curEvent && needsRefresh) {
		setEvent(curEvent, true);
	}
}

function setEvent(newEvent, bypass = false) {
	if(!bypass && (curEvent == newEvent)) {
		// Same event, don't do anything
		return;
	}
	
	let cecap = curEvent.charAt(0).toUpperCase() + curEvent.slice(1);
	let necap = newEvent.charAt(0).toUpperCase() + newEvent.slice(1);
	document.getElementsByClassName("selEv"+cecap)[0].classList.remove("selEvSelected");
	document.getElementsByClassName("selEv"+necap)[0].classList.add("selEvSelected");
	
	curEvent = newEvent;
	curEventData = eventData[curEvent];
	
	let medianTime = Math.round(Math.exp(params[curEvent].mu))/100
	document.getElementById("medianTime").textContent = medianTime.toFixed(2)+"s";
	let dnfRate = 100*params[curEvent].alep/(params[curEvent].alep+params[curEvent].bet)
	document.getElementById("dnfRate").textContent = dnfRate.toPrecision(2)+"%";
	
	recalcArea();
}

function recalcArea() {
	let svg = document.getElementById("tgChartArea").children[0];
	let polyline = svg.children[0];
	let polygon = svg.children[1];
	let pointStr = modeCDF ? curEventData.cdfPoints : curEventData.pdfPoints;
	polyline.setAttribute('points', pointStr);
	polygon.setAttribute('points', pointStr+"1000,610 0,610");
	
	let xTicksArr = calcTicks(curEventData.leftbound, curEventData.rightbound);
	let yTicksArr = calcTicks(0, modeCDF ? 1 : curEventData.maxPdf);
	let xTicksDiv = document.getElementById("tgAxisXTicks");
	let yTicksDiv = document.getElementById("tgAxisYTicks");
	
	yTicksDiv.replaceChildren();
	for(let a of yTicksArr.reverse()) {
		let tickDiv = document.createElement("div");
		tickDiv.classList.add("tgAxisYTick");
		let tickSpan = document.createElement("span");
		tickSpan.textContent = formatExp(a[0], a[1]+2)+"%";
		let tickDiv2 = document.createElement("div");
		tickDiv.replaceChildren(tickSpan, tickDiv2);
		yTicksDiv.appendChild(tickDiv);
	}
	// TODO: use calc() to get this a bit more exact
	let yTickMax = yTicksArr[0][0] * Math.pow(10, yTicksArr[0][1]);
	yTicksDiv.style.height = 100*yTickMax/(modeCDF ? 1 : curEventData.maxPdf)+"%";
	
	xTicksDiv.replaceChildren();
	for(let a of xTicksArr) {
		let tickDiv = document.createElement("div");
		tickDiv.classList.add("tgAxisXTick");
		let tickDiv2 = document.createElement("div");
		let tickSpan = document.createElement("span");
		tickSpan.textContent = formatExp(a[0], a[1]-2)+"s";
		tickDiv.replaceChildren(tickDiv2, tickSpan);
		xTicksDiv.appendChild(tickDiv);
	}
	// TODO: same as above
	let xTickMin = xTicksArr[0][0] * Math.pow(10, xTicksArr[0][1]);
	let xTickMax = xTicksArr[xTicksArr.length-1][0] * Math.pow(10, xTicksArr[xTicksArr.length-1][1]);
	xTicksDiv.style.width = 100*(xTickMax-xTickMin)/(curEventData.rightbound-curEventData.leftbound)+"%";
	xTicksDiv.style.paddingLeft = 100*(xTickMin-curEventData.leftbound)/(curEventData.rightbound-curEventData.leftbound)+"%";
	
	document.getElementById("tgAxisYLabel").textContent = modeCDF ? "Probability of getting time or better" : "Probability of getting time";
}

// Add tooltip
document.addEventListener("DOMContentLoaded", (e) => {
	modeCDF = document.getElementById("cumulativeInput").checked;
	document.getElementById("cumulativeInput").addEventListener("change", function (e) {
		modeCDF = this.checked;
		recalcArea();
	});
	
	if(eventData[curEvent] === undefined) {
		console.log("Not ready yet!");
		needsRefresh = true;
	} else {
		setEvent(curEvent, true);
	}
	
	const chartArea = document.getElementById("tgChartArea");
	const chartTip = document.getElementById("tgChartTooltip");
	chartArea.addEventListener("pointermove", (e) => {
		chartTip.style.left = e.clientX+"px";
		chartTip.style.top = e.clientY+"px";
		chartRect = chartArea.getBoundingClientRect();
		xPct = (e.clientX - chartRect.x)/chartRect.width;
		yPct = (e.clientY - chartRect.y)/chartRect.height;
		x = Math.round(xPct*curEventData.points);
		x = Math.max(0, Math.min(x, curEventData.points-1)); // Keep it in bounds
		xReal = ((curEventData.leftbound + xPct*(curEventData.rightbound-curEventData.leftbound))/100).toFixed(2);
		y = modeCDF ? (curEventData.cdf[x]*100).toFixed(2) : (curEventData.pdf[x]*100).toPrecision(3)
		
		chartTip.textContent = "("+xReal+", "+y+"%)";
	});
});

// Utility functions for axis calculations

function calcTicks(min, max) {
	b = Math.floor(Math.log10(max));
	a1 = min/Math.pow(10, b);
	a2 = max/Math.pow(10, b);
	result = [];
	
	intDiff = Math.floor(a2)-Math.ceil(a1);
	if(intDiff >= 3 && intDiff <= 8) {
		for(i = 0; i <= intDiff; i++) {
			result.push([Math.ceil(a1)+i, b]);
		}
		return result;
	}
	
	if(intDiff < 3) {
		a1 *= 10;
		a2 *= 10;
		b -= 1;
	}
	intDiff5 = Math.floor(a2/5)-Math.ceil(a1/5);
	if(intDiff5 >= 3) {
		for(i = 0; i <= intDiff5; i++) {
			result.push([Math.ceil(a1/5)*5 + i*5, b]);
		}
		return result;
	} else {
		// Going by twos better work!
		intDiff2 = Math.floor(a2/2)-Math.ceil(a1/2);
		for(i = 0; i <= intDiff2; i++) {
			result.push([Math.ceil(a1/2)*2+i*2, b]);
		}
		return result;
	}
}

function formatExp(base, pow) {
	if(base == 0) {
		return "0";
	} else if(pow >= 0) {
		return base + "0".repeat(pow);
	} else {
		const baseStr = base.toString();
		const decimalPos = baseStr.length + pow;
		
		if(decimalPos <= 0) {
			// Need leading zeros: 0.00...base
			pw = -pow - baseStr.length;
			return "0." + "0".repeat(pw) + baseStr;
		} else {
			// Insert decimal within the number
			return baseStr.slice(0, decimalPos) + "." + baseStr.slice(decimalPos);
		}
	}
}
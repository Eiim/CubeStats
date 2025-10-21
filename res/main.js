function openSidebar(){
	console.log("Showing sidebar")
	const sidebar = document.getElementById("sidebar")
	function sidebarToggleListener(event) {
		console.log("Received event")
		if(!sidebar.contains(event.target) && event.target !== document.getElementById("sidebarToggle")) {
			console.log("Hiding sidebar");
			sidebar.classList.remove("show");
			document.removeEventListener("click", sidebarToggleListener);
		}
	}
	
	sidebar.classList.add("show");
	document.addEventListener("click", sidebarToggleListener);
}
document.addEventListener("DOMContentLoaded", (event) => {
	const sidebarToggle = document.getElementById("sidebarToggle");
	if(sidebarToggle != null) {sidebarToggle.addEventListener("click", openSidebar);}
	
	for(e of document.getElementsByClassName("date")) {
		// Temporal has more consistent formatting, but is too new to be relied upon
		if("Temporal" in window && "PlainDate" in Temporal) {
			e.textContent = Temporal.PlainDate.from(e.textContent).toLocaleString()
		} else {
			[year, month, day] = e.textContent.split('-');
			e.textContent = (new Date(+year, month-1, +day)).toLocaleDateString()
		}
	}
});
document.addEventListener("DOMContentLoaded", (event) => {
	const popup = document.getElementById("mainSearchPopup");
	const searchBar = document.getElementById("mainSearch")
	
	async function search(query) {
		if(query.length < 2) {
			popup.style.display = "none";
			popup.innerHTML = "";
			return;
		}
		url = "/searchapi?f=json&t=person&q=" + encodeURIComponent(query);
		const response = await fetch(url);
		if(!response.ok) {
			console.error("Issue requesting search for "+query);
			console.error(response);
		} else {
			const results = await response.json();
			if(results.length == 0) {
				popup.style.display = "none";
				popup.innerHTML = "";
			} else {
				popup.style.display = "block";
				popup.innerHTML = "";
				for(r of results) {
					id = r.result;
					let aElem = document.createElement("a");
					aElem.href = "/person/"+id;
					let divElem = document.createElement("div");
					divElem.classList.add("mainSearchResult");
					divElem.textContent = id.toUpperCase();
					aElem.appendChild(divElem);
					popup.appendChild(aElem);
				}
			}
		}
	}
	
	search(searchBar.value)
	
	let debounceTimer;
	searchBar.addEventListener("keydown", () => {
		clearTimeout(debounceTimer);
		debounceTimer = setTimeout(() => {
			search(searchBar.value);
		}, 200);
	});
	
	document.getElementById("mainSearchForm").addEventListener("submit", async (e) => {
		e.preventDefault();
		url = "/searchapi?f=cs_url&n=1&q=" + encodeURIComponent(searchBar.value);
		const response = await fetch(url);
		if(response.ok) {
			location.href = await response.text();
		}
	})
});
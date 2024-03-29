

// IMPORTANT: notice the next request is scheduled only after the
//            previous request is fully processed either successfully
//	      or not.

function refreshVotes() {
	var request = new XMLHttpRequest();

	request.onload = function upDate() {
		document.getElementById("votes").innerHTML = this.responseText;
		setTimeout(refreshVotes, 1500);
	};

	request.ontimeout = function timeoutCase() {
		document.getElementById("votes").innerHTML = "No response from server, still trying ...";
		setTimeout(refreshVotes, 200);
	};

	request.onerror = function errorCase() {
		document.getElementById("votes").innerHTML = "No response from server, still trying ...";
		setTimeout(refreshVotes, 5000);
	};

	request.open("GET", "/votes", true);
	request.timeout = 5000;
	request.send();
}

function voteFor(option) {
	var request = new XMLHttpRequest();
	request.open("PUT", "/votes/" + option, true);
	request.send();
	var vBoard = document.getElementById("votes");
	vBoard.innerHTML = vBoard.innerHTML + "<p>Casting your vote ... Please wait.";
}



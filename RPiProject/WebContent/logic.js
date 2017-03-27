var ws;
var inputPinsState = true;
changeConnectionState("Connecting");
var clsRgx = /((pinOn)|(pinOff)|(pinToOn)|(pinToOff))/g;
var stateManagerWS = "ws://" + (location.href.toString().replace(/http(s?)\:\/\//, "").replace("/index.html", "").replace(/\/$/,"") + "/StateManager");
var logs = (location.href.toString().replace("/index.html","").replace(/\/$/,"") + "/logs/logs.html");
var config = (location.href.toString().replace("/index.html","").replace(/\/$/,"") + "/config/config.html");

document.addEventListener("DOMContentLoaded",
		function() {
			Array.from(document.querySelectorAll(".outPin")).forEach(function(e) {
				e.addEventListener("click", function(evt) {
					var elm = evt.target;
					if (!elm.unknownPinState()) {
						elm.pinStateChange();
					}
				});
			});
			document.querySelector("#configBtn").addEventListener(
					"click",
					function() {
						if(ws){
							ws.close();
						}
						location.href = config;
					});

			document.querySelector("#logsBtn").addEventListener(
					"click",
					function() {
						if(ws){
							ws.close();
						}
						location.href = logs;
					});

			ws = new WebSocket(stateManagerWS);

			ws.onopen = function() {
				changeConnectionState("Connected");
			}

			ws.onclose = function() {
				changeConnectionState("Disconnected");
			}

			ws.onmessage = function(e) {
				var d;
				if (e && e.data && (d = JSON.parse(e.data))) {
					if (d.tp == "PIN_INFO") {
						var elm = document.getElementById("pin" + d.index);
						if (elm) {
							if (!inputPinsState && d.index > 8) {
								elm.setPinState(!d.state);
							} else {
								elm.setPinState(d.state);
							}
						} else {
							console.error("state update for unknown pin",
									d.index);
						}
					} else if (d.tp == "CONFIG") {
						var reverse = inputPinsState !== d.inputPinsState;
						inputPinsState = d.inputPinsState;
						if (reverse) {
							for (var i = 9; i <= 16; i++) {
								var elm = document.getElementById("pin" + i);
								elm.setPinState(!elm.isPinOn());
							}
						}
					} else {
						console.error("unknown message type", d.tp)
					}
				} else {
					console.error("invalid message received", e);
				}
			}

		});

HTMLElement.prototype.unknownPinState = function() {
	return this.classList
			&& (this.classList.contains("pinToOn") || this.classList
					.contains("pinToOff"));
}

HTMLElement.prototype.isPinOn = function() {
	return this.className.indexOf("pinOn") != -1;
}

HTMLElement.prototype.pinStateChange = function() {
	var index;
	if (this.id && !isNaN(index = parseInt(this.id.replace(/\D/g, "")))) {
		if (index >= 1 && index <= 8) {
			sendMsg({
				tp : "PIN_INFO",
				index : index,
				state : !this.isPinOn(),
				mode : 1
			});
		} else {
			console.error("ERROR, invalid pin index", index);
		}
	} else {
		console.error("ERROR, unable to get pin index from element", this);
	}
	this.className = this.className.replace(clsRgx,
			(this.isPinOn() ? "pinToOff" : "pinToOn"));
}

HTMLElement.prototype.setPinState = function(state) {
	this.className = this.className.replace(clsRgx,
			(state ? "pinOn" : "pinOff"));
}

function changeConnectionState(cls) {
	var elm = document.getElementById("stateTxt");
	if (elm) {
		elm.className = "stateTxt" + cls;
	}
	var elm = document.getElementById("stateIndicator");
	if (elm) {
		elm.className = "stateIndicator" + cls;
	}
}

function sendMsg(msg) {
	if (ws && ws.readyState == 1) {
		ws.send(JSON.stringify(msg));
	} else {
		console.error("ERROR, unable to send message", msg,
				"the websocked is not open", ws.readyState);
	}
}
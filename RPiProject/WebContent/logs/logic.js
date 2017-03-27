var ws;
var home = location.href.replace("logs/logs.html", "index.html");
var config = location.href.replace("logs/logs.html", "config/config.html");
$(document).ready(
		function() {
			$("#homeBtn").click(function() {
				if(ws){
					ws.close();
				}
				location.href = home;
			});
			$("#configBtn").click(function() {
				if(ws){
					ws.close();
				}
				location.href = config;
			});

			$("#exportBtn").click(
					function() {
						var csv = "WHO, WHEN, WHAT";
						$("tbody tr").each(function() {
							csv += "\n"+$(this).find("td:eq(0)").text();
							csv += ","+$(this).find("td:eq(1)").text();
							csv += ","+$(this).find("td:eq(2)").text();
						});
						var elm = $("<a href='data:text/plain;charset=utf-8,"+encodeURIComponent(csv)+"' download='logs.csv' style='display:none'></a>");
						$("body").append(elm);
						elm[0].click();
					});

			ws = new WebSocket(location.href.replace(/http(s?)\:\/\//, "ws://")
					.replace("logs/logs.html", "LogsManager"));
			ws.onmessage = function(e) {
				var d;
				if (e && e.data && (d = JSON.parse(e.data))) {
					if (d.tp == "LOG") {
						$("#logs").prepend(
								"<tr><td>" + d.who + "</td><td>"
										+ new Date(d.when) + "</td><td>"
										+ d.what + "</td></tr>");
						$('#logs tr:gt(1000)').remove();
					}
				} else {
					console.error("invalid message received", e);
				}
			}
		});

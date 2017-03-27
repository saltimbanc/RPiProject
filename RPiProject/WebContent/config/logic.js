var ws;
	
$(document).ready(function(){
	for(var i = 1; i <= 8;i++){
		$("#outputPins").append(pinComponent.replace(/NUMBER/g,i));
	}
	$("#homeBtn").click(function(){
		if(ws){
			ws.close();
		}
		location.href = location.href.replace("config/config.html","index.html");
	});
	$("#logsBtn").click(function(){
		if(ws){
			ws.close();
		}
		location.href = location.href.replace("config/config.html","logs/logs.html");
	});
	$("body").on("change", ".useDelay", function(){
		if($(this).prop("checked") && isNaN(parseInt($(this).parent().parent().find(".delayCon .delay").val()))){
			return;
		}
		updatePinState.call(this);
	});
	$("body").on("change", ".pinState", updatePinState);
	$("body").on("blur", ".delay", function(){
		var p = $(this).parents(".pinStateCon");
		var index = parseInt(p.attr("id").replace(/\D/g,""));
		if(!isNaN(index) && index >= 1 && index <= 8){
			if($("#pin"+index+"UseDelay").prop("checked")){
				updatePinState.call(this);
			}
		}
	});
	$("#stateMapping").change(function(){
		var o = {tp:"CONFIG", inputPinsState:($(this).val() == "on")};
		send(o);
	});
	
	
	for(var i = 1; i <= 16; i++){
		var type = i <= 8? "output":"input";
		var row = $(rowModel.replace(/PIN_TYPE/g,type).replace(/NUMBER/g, i));
		if(i > 8){
			row.append(resistance.replace(/PIN_TYPE/g,type).replace(/NUMBER/g, i));
		}
		$("#"+type+"PinsTB").append(row);
	}
	
	$("#saveMapping").click(function(){
		var pins = [];
		var unique = [];
		for(var i = 1; i <= 8;i++){
			var outputPin = {tp:"PIN_INFO", index: i, mode: 1, pi4j: $("#output_GPIO_"+i).val()};
			var inputPin = {tp:"PIN_INFO", index: (i+8), mode: 0, pi4j: $("#input_GPIO_"+(i+8)).val(), resistance: $("#input_resistance_"+(i+8)).val()};
			if(unique.indexOf(inputPin.pi4j) != -1 || unique.indexOf(outputPin.pi4j)!=-1){
				alert("Invalid pin mapping, multiple pins are mapped to the same Pi4J number");
				return;
			}
			unique.push(inputPin.pi4j);
			unique.push(outputPin.pi4j);
			pins.push(outputPin);
			pins.push(inputPin);
		}
		send({tp:"PINS_MAPPING", pins:pins});
		
	});
	
	ws = new WebSocket(location.href.replace(/http(s?)\:\/\//, "ws://").replace("config/config.html", "ConfigManager"));
	ws.onmessage = function(e){
		var d;
		if (e && e.data && (d = JSON.parse(e.data))) {
			if (d.tp == "PIN_INFO") {
				if(d.index >= 1 && d.index<=8){
					$("#pin"+d.index+"State").prop("checked", d.state);
					$("#pin"+d.index+"UseDelay").prop("checked", d.delay > 0);
					$("#pin"+d.index+"Delay").val(d.delay > 0?d.delay:"");
				}else{
					console.error("Invalid message received, invalid index", d.index);
				}
			}else if (d.tp == "CONFIG") {
				if(d.inputPinsState || typeof d.inputPinsState == "undefined"){
					$("#stateMapping").val("on");
				}else{
					$("#stateMapping").val("off");
				}
			}else if(d.tp == "PINS_MAPPING"){
				if(d.pins){
					d.pins.forEach(function(v){
						$("#"+(v.mode?"output":"input")+"_GPIO_"+(v.index)).val(v.pi4j);
						if(v.mode == 0){
							$("#input_resistance_"+(v.index)).val(v.resistance);
						}
					});
				}
			}else{
				console.error("unknown message type", d.tp)
			}
		}else{
			console.error("invalid message received", e);
		}
	}
});

function updatePinState(){
	var p = $(this).parents(".pinStateCon");
	var index = parseInt(p.attr("id").replace(/\D/g,""));
	if(!isNaN(index) && index >= 1 && index <= 8){
		var delay = parseInt($("#pin"+index+"Delay").val());
		var o = {tp:"PIN_INFO", index:index, state: $("#pin"+index+"State").prop("checked"), delay:($("#pin"+index+"UseDelay").prop("checked") && !isNaN(delay)?delay:0)};
		send(o);
	}else{
		console.error("invalid index: " + index)
	}
}

function send(msg){
	if (ws && ws.readyState == 1) {
		ws.send(JSON.stringify(msg));
	} else {
		console.error("ERROR, unable to send message", msg,
				"the websocked is not open", ws.readyState);
	}
}


var rowModel = `<tr>
							<td>NUMBER</td>
							<td>
							<select id="PIN_TYPE_GPIO_NUMBER">
								<option value="0">0</option>
								<option value="1">1</option>
								<option value="2">2</option>
								<option value="3">3</option>
								<option value="4">4</option>
								<option value="5">5</option>
								<option value="6">6</option>
								<option value="7">7</option>
								<option value="8">8</option>
								<option value="9">9</option>
								<option value="10">10</option>
								<option value="11">11</option>
								<option value="12">12</option>
								<option value="13">13</option>
								<option value="14">14</option>
								<option value="15">15</option>
								<option value="16">16</option>
								<option value="17">17</option>
								<option value="18">18</option>
								<option value="19">19</option>
								<option value="20">20</option>
								<option value="21">21</option>
								<option value="22">22</option>
								<option value="23">23</option>
								<option value="24">24</option>
								<option value="25">25</option>
								<option value="26">26</option>
								<option value="27">27</option>
								<option value="28">28</option>
								<option value="29">29</option>
								<option value="30">30</option>
								<option value="31">31</option>
							</select>
							</td>
						</tr>`
	var resistance = `<td>
							<select id="PIN_TYPE_resistance_NUMBER">
								<option value="0">Off</option>
								<option value="1">Pull Up</option>
								<option value="-1">Pull Down</option>
							</select>
						</td>`;


var pinComponent = `<div class="pinStateCon" id="pinNUMBER">
	<div class="pinStateTitle con">Pin NUMBER</div>
	<div class="con">
		<div class="wrap defaultStateLabel">Default State:</div>
		<div class="onoffswitch wrap">
			<input type="checkbox" name="onoffswitch" class="onoffswitch-checkbox pinState" id="pinNUMBERState">
			<label class="onoffswitch-label" for="pinNUMBERState">
				<span class="onoffswitch-inner"></span>
				<span class="onoffswitch-switch"></span>
			</label>
		</div>
	</div>
	<div class="con useDelayCon">
		<div class="squaredOne wrap">
			<input class="useDelay" type="checkbox" value="None" id="pinNUMBERUseDelay" name="check" />
			<label for="pinNUMBERUseDelay"></label>
		</div>
		<div class="useDelayLabel wrap">Momentary</div>
		<div class="wrap delayCon"><input id="pinNUMBERDelay" type="numeric" class="delay" placeholder="Delay"> ms</div>
	</div>
</div>`
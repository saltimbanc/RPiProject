package rpi.ser;

import java.util.ArrayList;

public class Config {

	public static final String type = "CONFIG";
	final String tp = type;
	public boolean inputPinsState = true;
	public ArrayList<PinInfo> outputPins = new ArrayList<>();
	@Override
	public String toString() {
		return "INPUT PINS STATE: " + (inputPinsState ? "HIGH = ON, LOW = OFF" : "HIGH = OFF, LOW = ON") + "\nOUTPUT PINS DEFAULT STATE: " + outputPins;
	}
	
	public Config(){}
	
	public Config(boolean inputPinsState){
		this.inputPinsState = inputPinsState;
		outputPins = null;
	}

	

}

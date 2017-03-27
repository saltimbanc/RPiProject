package rpi.ser;

public class Log {

	public static final String type = "LOG";
	final String tp = type;

	public String who;
	public long when;
	public String what;

	public Log(String who, long when, String what) {
		this.who = who;
		this.when = when;
		this.what = what;
	}

	public static String userRequest(PinInfo pin) {
		return "user request to set pin: " + pin.index + " to: " + (pin.state ? "HIGH" : "LOW");
	}

	public static String systemPinEvent(PinInfo pin) {
		return "pin: " + pin.index + " state was changed to: " + (pin.state ? "HIGH" : "LOW");
	}

	public static String userDefaultPinState(PinInfo pin) {
		return "pin: " + pin.index + " startup state was changed to: " + (pin.state ? "HIGH" : "LOW") + (pin.delay > 0 ? " and momentary delay was set to: " + pin.delay : "");
	}

	public static String userDefaultInputPins(boolean mode) {
		return "input pins mode was changed to: " + (mode ? " HIGH = ON. LOW = OFF" : " HIGH = OFF. LOW = ON");
	}

	public static String mappingChanged(PinsMapping pinsMapping) {
		return "pins mapping was changed to: " + pinsMapping;
	}

	public static String initPin(PinInfo pin) {
		return "initializing pin: " + pin.index + " to state: " + (pin.state ? "HIGH" : "LOW");// + (pin.delay > 0 ? " momentary: " + pin.delay : "");
	}

	public static String delayDone(PinInfo pin) {
		return "pin " + pin.index + " momentary delay ("+pin.delay+" mills) done. Reversing state back to: " + (pin.state ? "HIGH" : "LOW");
	}

}

package rpi.ser;

import com.pi4j.io.gpio.GpioPinDigital;
import com.pi4j.io.gpio.PinPullResistance;

import rpi.tools.Utils;

public class PinInfo {
	public static final String type = "PIN_INFO";
	final String tp = type;

	public int index;
	public boolean state;
	public int delay;
	public int mode;
	public int resistance;
	public int pi4j;

	public PinInfo() {
	}

	public PinInfo(int index, boolean state, int delay) {
		this.index = index;
		this.state = state;
		this.delay = delay;
	}

	public PinInfo(GpioPinDigital pin, boolean state, int delay) {
		this(Utils.getIndex(pin), state, delay);
	}

	public PinInfo(int index, boolean state) {
		this(index, state, 0);
	}

	public PinInfo(GpioPinDigital pin, boolean state) {
		this(pin, state, 0);
	}

	public PinInfo(int index, int pi4j) {
		this.index = index;
		this.pi4j = pi4j;
		this.mode = 1;
	}

	public PinInfo(int index, int pi4j, int resistance) {
		this.index = index;
		this.pi4j = pi4j;
		this.mode = 0;
		this.resistance = resistance;
	}

	public boolean validPinMapping() {
		return (mode == 1 && index >= 1 && index <= Utils.outputPinsCount) || (mode == 0 && index > Utils.outputPinsCount && index <= Utils.pinsCount);
	}

	public PinPullResistance getResistance() {
		switch (resistance) {
		case -1:
			return PinPullResistance.PULL_DOWN;
		case 1:
			return PinPullResistance.PULL_UP;
		default:
			return PinPullResistance.OFF;
		}
	}

	@Override
	public String toString() {
		return "Index: " + index + " state: " + (state ? "ON" : "OFF") + " mode: " + (mode == 0 ? "input" : "output") + " delay: " + delay + " pi4j: " + pi4j + " resistance "
				+ resistance + "\n";
	}

}

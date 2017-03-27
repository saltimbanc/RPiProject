package rpi.ser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import rpi.tools.Utils;

public class PinsMapping {

	public static final String type = "PINS_MAPPING";
	final String tp = type;
	public ArrayList<PinInfo> pins = new ArrayList<>();

	public boolean valid() {
		int totalOutputPins = 0;
		int totalInputPins = 0;
		Set<Integer> uniqueWeb = new HashSet<>();
		Set<Integer> uniquePi4j = new HashSet<>();
		if (pins != null && pins.size() == Utils.pinsCount) {
			for (int i = 0; i < pins.size(); i++) {
				PinInfo pin = pins.get(i);
				if (Utils.pi4jNumbers.containsKey(pin.pi4j) && uniqueWeb.add(pin.index) && uniquePi4j.add(pin.pi4j) && pin.validPinMapping()) {
					if (pin.mode == 0) {
						totalInputPins++;
					} else if (pin.mode == 1) {
						totalOutputPins++;
					}
				} else {
					return false;
				}
			}
		}
		return totalInputPins == Utils.inputPinsCount && totalOutputPins == Utils.outputPinsCount && uniquePi4j.size() == uniqueWeb.size() && uniquePi4j.size() == Utils.pinsCount;
	}

	@Override
	public String toString() {
		String str = "NULL";
		if (pins != null) {
			str = "";
			for (PinInfo pin : pins) {
				str += (str.isEmpty() ? "" : ", ") + "web: " + pin.index + " pi4j: " + pin.pi4j;
			}
		}
		return str;
	}

}

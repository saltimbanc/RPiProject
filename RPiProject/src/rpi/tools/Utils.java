package rpi.tools;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigital;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import rpi.LogsManager;
import rpi.StateManager;
import rpi.ser.Config;
import rpi.ser.Log;
import rpi.ser.PinInfo;
import rpi.ser.PinsMapping;

public class Utils {
	public static final ConcurrentHashMap<Integer, GpioPinDigitalOutput> outputPins = new ConcurrentHashMap<>();
	public static final ConcurrentHashMap<Integer, GpioPinDigitalInput> inputPins = new ConcurrentHashMap<>();
	public static final CopyOnWriteArrayList<GpioPinDigital> allPins = new CopyOnWriteArrayList<>();
	public static final ConcurrentHashMap<Integer, Pin> pi4jNumbers = new ConcurrentHashMap<>();
	public static final int outputPinsCount = 8;
	public static final int inputPinsCount = 8;
	public static final int pinsCount = outputPinsCount + inputPinsCount;
	public static final CopyOnWriteArrayList<Log> logsQueue = new CopyOnWriteArrayList<>();

	public static void initPins(boolean force) {
		if (force || (outputPins.size() != outputPinsCount || inputPins.size() != inputPinsCount)) {

			pi4jNumbers.put(0, RaspiPin.GPIO_00);
			pi4jNumbers.put(1, RaspiPin.GPIO_01);
			pi4jNumbers.put(2, RaspiPin.GPIO_02);
			pi4jNumbers.put(3, RaspiPin.GPIO_03);
			pi4jNumbers.put(4, RaspiPin.GPIO_04);
			pi4jNumbers.put(5, RaspiPin.GPIO_05);
			pi4jNumbers.put(6, RaspiPin.GPIO_06);
			pi4jNumbers.put(7, RaspiPin.GPIO_07);
			pi4jNumbers.put(8, RaspiPin.GPIO_08);
			pi4jNumbers.put(9, RaspiPin.GPIO_09);
			pi4jNumbers.put(10, RaspiPin.GPIO_10);
			pi4jNumbers.put(11, RaspiPin.GPIO_11);
			pi4jNumbers.put(12, RaspiPin.GPIO_12);
			pi4jNumbers.put(13, RaspiPin.GPIO_13);
			pi4jNumbers.put(14, RaspiPin.GPIO_14);
			pi4jNumbers.put(15, RaspiPin.GPIO_15);
			pi4jNumbers.put(16, RaspiPin.GPIO_16);
			pi4jNumbers.put(17, RaspiPin.GPIO_17);
			pi4jNumbers.put(18, RaspiPin.GPIO_18);
			pi4jNumbers.put(19, RaspiPin.GPIO_19);
			pi4jNumbers.put(20, RaspiPin.GPIO_20);
			pi4jNumbers.put(21, RaspiPin.GPIO_21);
			pi4jNumbers.put(22, RaspiPin.GPIO_22);
			pi4jNumbers.put(23, RaspiPin.GPIO_23);
			pi4jNumbers.put(24, RaspiPin.GPIO_24);
			pi4jNumbers.put(25, RaspiPin.GPIO_25);
			pi4jNumbers.put(26, RaspiPin.GPIO_26);
			pi4jNumbers.put(27, RaspiPin.GPIO_27);
			pi4jNumbers.put(28, RaspiPin.GPIO_28);
			pi4jNumbers.put(29, RaspiPin.GPIO_29);
			pi4jNumbers.put(30, RaspiPin.GPIO_30);
			pi4jNumbers.put(31, RaspiPin.GPIO_31);

			PinsMapping pinsMapping = null;
			try (DB db = new DB()) {
				pinsMapping = db.getPinsMapping();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (pinsMapping != null && pinsMapping.valid()) {
				clear();
				for (PinInfo pin : pinsMapping.pins) {
					if (pin.mode == 1) {
						try {
							outputPins.put(pin.index, GpioFactory.getInstance().provisionDigitalOutputPin(pi4jNumbers.get(pin.pi4j)));
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (pin.mode == 0) {
						try {
							GpioPinDigitalInput inputPin = GpioFactory.getInstance().provisionDigitalInputPin(pi4jNumbers.get(pin.pi4j));
							inputPins.put(pin.index, inputPin);
							inputPin.setPullResistance(pin.getResistance());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

				allPins.addAll(outputPins.values());
				allPins.addAll(inputPins.values());

				setShutdown();
				StateManager.updateClients();
				setListeners();
			} else {
				System.err.println("!!!! INVALID PINS MAPPING: " + pinsMapping + " valid: " + (pinsMapping != null ? pinsMapping.valid() : "NULL") + " "
						+ (pinsMapping != null && pinsMapping.pins != null ? pinsMapping.pins.size() : "pins are NULL"));
				defaultConfiguration();
			}
		}
	}

	private static void clear() {
		removeListeners();
		for (GpioPinDigital pin : allPins) {
			try {
				pin.removeAllListeners();
				GpioFactory.getInstance().unprovisionPin(pin);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		outputPins.clear();
		inputPins.clear();
		allPins.clear();
	}

	private static void setShutdown() {
		for (GpioPinDigital pin : allPins) {
			try {
				pin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void defaultConfiguration() {
		addLog(new Log("system", System.currentTimeMillis(), "CHANGING MAPPING TO DEFAULT CONFIGURATION output 0 -> 8, input 21 -> 28"));
		clear();

		for (int i = 0; i < 8; i++) {
			try {
				outputPins.put(i + 1, GpioFactory.getInstance().provisionDigitalOutputPin(pi4jNumbers.get(i)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for (int i = 21; i < 29; i++) {
			try {
				inputPins.put(i - 12, GpioFactory.getInstance().provisionDigitalInputPin(pi4jNumbers.get(i), PinPullResistance.PULL_DOWN));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		allPins.addAll(outputPins.values());
		allPins.addAll(inputPins.values());
		setShutdown();
		StateManager.updateClients();
		setListeners();
	}

	public static int getIndex(GpioPinDigital pin) {
		Set<Map.Entry<Integer, ? extends GpioPinDigital>> entries = new HashSet<>();
		entries.addAll(outputPins.entrySet());
		entries.addAll(inputPins.entrySet());
		for (Map.Entry<Integer, ? extends GpioPinDigital> entry : entries) {
			if (entry.getValue().getPin().compareTo(pin.getPin()) == 0) {
				return entry.getKey();
			}
		}
		System.err.println("@Const.getIndex() unable to retrieve index for pin: " + pin.getName());
		return -1;
	}

	public static Config getConfiguration() {
		Config config = null;
		try (DB db = new DB()) {
			config = db.getConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
			config = new Config();
			for (int i = 1; i <= outputPinsCount; i++) {
				config.outputPins.add(new PinInfo(i, false));
			}
		}
		return config;
	}

	final static ArrayBlockingQueue<Log> listenersLogsQueue = new ArrayBlockingQueue<>(100);

	public static void setListeners() {
		removeListeners();
		if (!StateManager.clients.isEmpty()) {
			try {
				for (final GpioPinDigital pin : allPins) {
					final AtomicInteger count = new AtomicInteger(0);
					final AtomicLong lastUpdate = new AtomicLong(System.currentTimeMillis());
					keepGoing.set(false);
					try {
						listenersLogsQueue.add(new Log("", -1, ""));
					} catch (Exception e) {
						e.printStackTrace();
					}
					initLogsQueue();
					pin.addListener(new GpioPinListenerDigital() {
						@Override
						public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent evt) {
							long now = System.currentTimeMillis();
							count.incrementAndGet();
							if (now - lastUpdate.get() >= 2000) {
								if (count.get() != -1 && count.get() > 10) {
									System.err.println("*****WARNING Removeing listener from pin: " + getIndex(pin)
											+ " REASON: More then 10 updates in 2 seconds (the pin is in a floating state) *****");
									try {
										listenersLogsQueue.put(new Log("system", System.currentTimeMillis(), "*****WARNING Removeing listener from pin: " + getIndex(pin)
												+ " REASON: More then 10 updates in 2 seconds (the pin is in a floating state) *****"));
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									pin.removeAllListeners();
									count.set(-1);
									return;
								} else {
									lastUpdate.set(now);
									count.set(0);
								}
							}
							PinInfo pinInfo = new PinInfo(pin, evt.getState().isHigh());
							try {
								listenersLogsQueue.put(new Log("system", now, Log.systemPinEvent(pinInfo)));
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							StateManager.updateClients(pinInfo);
						}
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void removeListeners() {
		for (GpioPinDigital pin : allPins) {
			pin.removeAllListeners();
		}
	}

	public static void addLog(Log log) {
		addLog(log, false);
	}

	public static void addLog(Log log, boolean force) {
		LogsManager.updateClients(log);
		logsQueue.add(log);
		if (logsQueue.size() >= 50 || force) {
			try (DB db = new DB()) {
				db.addLogs(logsQueue);
				logsQueue.clear();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	static final AtomicBoolean keepGoing = new AtomicBoolean(true);

	public static void initLogsQueue() {
		new Thread(new Runnable() {
			public void run() {
				keepGoing.set(true);
				while (keepGoing.get()) {
					try {
						Log log = listenersLogsQueue.take();
						if (log.when != -1) {
							addLog(log);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				}
			}
		}).start();
	}
}

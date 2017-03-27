package rpi;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pi4j.io.gpio.GpioPinDigital;

import rpi.ser.Config;
import rpi.ser.Log;
import rpi.ser.PinInfo;
import rpi.tools.Utils;

@ServerEndpoint("/StateManager")
public class StateManager {

	public static final Set<Session> clients = new CopyOnWriteArraySet<>();
	public static final Object lock = new Object();
	public static Config config = null;
	public static final Object configLock = new Object();

	@OnOpen
	public void open(Session session) {
		clients.add(session);
		try {
			Utils.initPins(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (GpioPinDigital pin : Utils.allPins) {
			PinInfo pinInfo = new PinInfo(pin, pin.isHigh());
			try {
				synchronized (session) {
					session.getBasicRemote().sendText(new Gson().toJson(pinInfo));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		synchronized (configLock) {
			config = Utils.getConfiguration();
			try {
				synchronized (session) {
					session.getBasicRemote().sendText(new Gson().toJson(new Config(config.inputPinsState)));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (clients.size() == 1) {
				Utils.setListeners();
			}
		}
	}

	@OnClose
	public void close(Session session) {
		clients.remove(session);
		/*
		 * if (clients.isEmpty()) { Utils.removeListeners(); }
		 */
	}

	@OnError
	public void onError(Throwable error) {
		error.printStackTrace();
	}

	@OnMessage
	public void handleMessage(String message, Session session) {
		if (message != null && !message.trim().isEmpty()) {
			JsonObject obj = new Gson().fromJson(message, JsonObject.class);
			if (obj.has("tp")) {
				if (obj.get("tp").getAsString().equals(PinInfo.type)) {
					PinInfo pinInfo = new Gson().fromJson(message, PinInfo.class);
					Utils.addLog(new Log("user", System.currentTimeMillis(), Log.userRequest(pinInfo)));
					try {
						if (Utils.outputPins.containsKey(pinInfo.index)) {
							Utils.outputPins.get(pinInfo.index).setState(pinInfo.state);
							synchronized (configLock) {
								if (config != null && config.outputPins != null) {
									for (PinInfo pi : config.outputPins) {
										if (pi.index == pinInfo.index) {
											setPinDelay(pi, !pinInfo.state);
											break;
										}
									}
								}
							}
						} else {
							System.err.println("ERROR @StateManager.handleMessage, invalid request, pin: " + pinInfo.index + " is not an output pin");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					System.err.println("ERROR @StateManager.handleMessage, unknown message type: " + obj.get("tp").getAsString());
				}
			} else {
				System.err.println("ERROR @StateManager.handleMessage, unknown message format: " + message);
			}
		}
	}

	public static void updateClients(Object obj) {
		for (Session session : clients) {
			try {
				synchronized (session) {
					session.getBasicRemote().sendText(new Gson().toJson(obj));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void updateClients() {
		for (GpioPinDigital pin : Utils.allPins) {
			PinInfo pinInfo = new PinInfo(pin, pin.isHigh());
			updateClients(pinInfo);
		}
	}


	public static void updateConfig(PinInfo pinInfo) {
		synchronized (configLock) {
			for (PinInfo pi : config.outputPins) {
				if (config != null && config.outputPins != null) {
					if (pi.index == pinInfo.index) {
						pi.delay = pinInfo.delay;
					}
				}
			}
		}
	}

	public static void setPinDelay(final PinInfo pi, final boolean state) {
		if (pi.delay > 0) {
			new Thread(new Runnable() {
				public void run() {
					synchronized (Thread.currentThread()) {
						try {
							Thread.currentThread().wait(pi.delay);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						Utils.addLog(new Log("system", System.currentTimeMillis(), Log.delayDone(pi)), true);
						Utils.outputPins.get(pi.index).setState(state);
					}
				}
			}).start();
		}
	}

}

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

import rpi.ser.Config;
import rpi.ser.Log;
import rpi.ser.PinInfo;
import rpi.ser.PinsMapping;
import rpi.tools.DB;
import rpi.tools.Utils;

@ServerEndpoint("/ConfigManager")
public class ConfigManager {

	public static final Set<Session> clients = new CopyOnWriteArraySet<>();

	@OnOpen
	public void open(Session session) {
		clients.add(session);
		Config config = Utils.getConfiguration();
		for (PinInfo pinInfo : config.outputPins) {
			try {
				synchronized (session) {
					session.getBasicRemote().sendText(new Gson().toJson(pinInfo));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			synchronized (session) {
				session.getBasicRemote().sendText(new Gson().toJson(new Config(config.inputPinsState)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try (DB db = new DB()) {
			PinsMapping pinsMapping = db.getPinsMapping();
			synchronized (session) {
				session.getBasicRemote().sendText(new Gson().toJson(pinsMapping));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@OnClose
	public void close(Session session) {
		clients.remove(session);
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
					Utils.addLog(new Log("user", System.currentTimeMillis(), Log.userDefaultPinState(pinInfo)));
					if (pinInfo.index >= 1 && pinInfo.index <= Utils.outputPinsCount) {
						try (DB db = new DB()) {
							if (db.setOutputPinDefaultState(pinInfo)) {
								StateManager.updateConfig(pinInfo);
								updateClients(pinInfo);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						System.err.println("ERROR @ConfigManager.handleMessage, pin: " + pinInfo.index + " is not an output pin");
					}
				} else if (obj.get("tp").getAsString().equals(Config.type) && obj.has("inputPinsState")) {
					try (DB db = new DB()) {
						db.setInputPinMode(obj.get("inputPinsState").getAsBoolean());
						Utils.addLog(new Log("user", System.currentTimeMillis(), Log.userDefaultInputPins(obj.get("inputPinsState").getAsBoolean())));
						updateClients(obj);
						StateManager.updateClients(obj);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (obj.get("tp").getAsString().equals(PinsMapping.type)) {
					PinsMapping pinsMapping = new Gson().fromJson(message, PinsMapping.class);
					if (pinsMapping != null && pinsMapping.valid()) {
						try (DB db = new DB()) {
							if (db.changeMapping(pinsMapping)) {
								Utils.initPins(true);
								updateClients(pinsMapping);
								Utils.addLog(new Log("system", System.currentTimeMillis(), Log.mappingChanged(pinsMapping)));
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} else {
					System.err.println("ERROR @ConfigManager.handleMessage, unknown message type: " + obj.get("tp").getAsString());
				}
			} else {
				System.err.println("ERROR @ConfigManager.handleMessage, unknown message format: " + message);
			}
		}
	}

	public void updateClients(Object obj) {
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

}

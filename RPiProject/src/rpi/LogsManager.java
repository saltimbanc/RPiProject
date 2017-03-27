package rpi;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;

import rpi.ser.Log;
import rpi.tools.DB;
import rpi.tools.Utils;

@ServerEndpoint("/LogsManager")
public class LogsManager {

	public static final Set<Session> clients = new CopyOnWriteArraySet<>();

	@OnOpen
	public void open(Session session) {
		clients.add(session);
		try (DB db = new DB()) {
			ArrayList<Log> logs = db.getLogs();
			for (int i = logs.size() - 1; i >= 0; i--) {
				synchronized (session) {
					session.getBasicRemote().sendText(new Gson().toJson(logs.get(i), Log.class));
				}
			}
			for (Log log : Utils.logsQueue) {
				synchronized (session) {
					session.getBasicRemote().sendText(new Gson().toJson(log, Log.class));
				}
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
}

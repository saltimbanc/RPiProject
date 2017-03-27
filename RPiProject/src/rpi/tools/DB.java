package rpi.tools;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import rpi.ser.Config;
import rpi.ser.Log;
import rpi.ser.PinInfo;
import rpi.ser.PinsMapping;

public class DB implements Closeable {

	DataSource ds = null;
	InitialContext initContext = null;

	public DB() {
		try {
			initContext = new InitialContext();
			ds = (DataSource) initContext.lookup("java:comp/env/jdbc/DB");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void generateDB() {
		try (Connection con = ds.getConnection()) {
			con.setAutoCommit(false);
			try (Statement st = con.createStatement()) {
				int count = 0;
				try (ResultSet rs = st.executeQuery(
						"SELECT count(*) as total FROM sqlite_master WHERE type='table' AND name IN('outputPins', 'inputPins', 'outputPinsMapping', 'inputPinsMapping', 'logs')")) {
					if (rs.next()) {
						count = rs.getInt("total");
					}
				}
				if (count < 5) {
					st.addBatch("DROP TABLE IF EXISTS outputPins");
					st.addBatch("DROP TABLE IF EXISTS inputPins");
					st.addBatch("DROP TABLE IF EXISTS outputPinsMapping");
					st.addBatch("DROP TABLE IF EXISTS inputPinsMapping");
					st.addBatch("DROP TABLE IF EXISTS logs");
					st.addBatch("CREATE TABLE outputPins(pin INTEGER PRIMARY KEY, state INTEGER, delay INTEGER)");
					st.addBatch("CREATE TABLE inputPins(state INTEGER)");
					st.addBatch("CREATE TABLE outputPinsMapping(web INTEGER, pi4j INTEGER)");
					st.addBatch("CREATE TABLE inputPinsMapping(web INTEGER, pi4j INTEGER, resistance INTEGER)");
					st.addBatch("CREATE TABLE logs(who TEXT, whenn INTEGER, what TEXT)");
					for (int i = 1; i <= Utils.outputPinsCount; i++) {
						st.addBatch("INSERT INTO outputPins(pin, state, delay) VALUES(" + i + ", 0, 0)");
						st.addBatch("INSERT INTO outputPinsMapping(web, pi4j) VALUES(" + i + "," + (i - 1) + ")");
					}
					for (int i = Utils.outputPinsCount + 1; i <= Utils.pinsCount; i++) {
						st.addBatch("INSERT INTO inputPinsMapping(web, pi4j, resistance) VALUES(" + (i) + "," + (12 + i) + ", -1)");
					}
					st.addBatch("INSERT INTO inputPins(state) VALUES(1)");
					st.executeBatch();
					con.commit();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Config getConfiguration() {
		Config config = new Config();
		try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
			try (ResultSet rs = st.executeQuery("SELECT * FROM outputPins")) {
				while (rs.next()) {
					config.outputPins.add(new PinInfo(rs.getInt("pin"), rs.getBoolean("state"), rs.getInt("delay")));
				}
			}
			try (ResultSet rs = st.executeQuery("SELECT state FROM inputPins")) {
				if (rs.next()) {
					config.inputPinsState = rs.getBoolean("state");
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return config;
	}

	public boolean changeMapping(PinsMapping mapping) {
		try (Connection con = ds.getConnection();) {
			con.setAutoCommit(false);
			for (PinInfo info : mapping.pins) {
				boolean input = info.mode == 0;
				try (PreparedStatement ps = con
						.prepareStatement("UPDATE " + (input ? "inputPinsMapping SET pi4j = ?, resistance = ?" : "outputPinsMapping SET pi4j = ?") + " WHERE web = ?")) {
					ps.setInt(1, info.pi4j);
					if (input) {
						ps.setInt(2, info.resistance);
					}
					ps.setInt(input ? 3 : 2, info.index);
					ps.addBatch();
					ps.executeBatch();
				}
			}
			con.commit();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public PinsMapping getPinsMapping() {
		PinsMapping pinsMapping = new PinsMapping();
		try (Connection con = ds.getConnection();) {
			try (Statement st = con.createStatement()) {
				try (ResultSet rs = st.executeQuery("SELECT * FROM outputPinsMapping")) {
					while (rs.next()) {
						pinsMapping.pins.add(new PinInfo(rs.getInt("web"), rs.getInt("pi4J")));
					}
				}
			}
			try (Statement st = con.createStatement()) {
				try (ResultSet rs = st.executeQuery("SELECT * FROM inputPinsMapping")) {
					while (rs.next()) {
						pinsMapping.pins.add(new PinInfo(rs.getInt("web"), rs.getInt("pi4J"), rs.getInt("resistance")));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pinsMapping;
	}

	public boolean setOutputPinDefaultState(PinInfo pin) {
		try (Connection con = ds.getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE outputPins SET state = ?, delay = ? WHERE pin = ?")) {
			ps.setBoolean(1, pin.state);
			ps.setInt(2, pin.delay);
			ps.setInt(3, pin.index);
			ps.execute();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void setInputPinMode(boolean mode) {
		try (Connection con = ds.getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE inputPins SET state = ?")) {
			ps.setBoolean(1, mode);
			ps.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	AtomicInteger logsCount = new AtomicInteger(0);

	public void addLogs(CopyOnWriteArrayList<Log> logs) {
		try (Connection con = ds.getConnection();) {
			con.setAutoCommit(false);
			try (PreparedStatement ps = con.prepareStatement("INSERT INTO logs(who, whenn, what) VALUES(?,?,?)")) {
				for (Log log : logs) {
					ps.setString(1, log.who);
					ps.setLong(2, log.when);
					ps.setString(3, log.what);
					ps.addBatch();
				}
				ps.executeBatch();
			}
			if (logsCount.incrementAndGet() >= 400) {
				try (Statement st = con.createStatement()) {
					st.addBatch("DELETE FROM logs WHERE whenn < (SELECT whenn FROM logs ORDER BY whenn DESC LIMIT 1000, 1)");
					logsCount.set(0);
					st.executeBatch();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			con.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public ArrayList<Log> getLogs() {
		ArrayList<Log> logs = new ArrayList<>();
		try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
			try (ResultSet rs = st.executeQuery("SELECT * FROM logs ORDER BY whenn DESC LIMIT 1000")) {
				while (rs.next()) {
					logs.add(new Log(rs.getString("who"), rs.getLong("whenn"), rs.getString("what")));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return logs;
	}

	@Override
	public void close() throws IOException {
		try {
			if (initContext != null) {
				initContext.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

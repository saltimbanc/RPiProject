package rpi;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.pi4j.io.gpio.GpioFactory;

import rpi.ser.Config;
import rpi.ser.Log;
import rpi.ser.PinInfo;
import rpi.tools.Utils;
import rpi.tools.DB;

@WebListener
public class StartStopListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		GpioFactory.getInstance().shutdown();
		Utils.addLog(new Log("system", System.currentTimeMillis(), "SYSTEM SHUTDOWN"), true);
	}

	@Override
	public void contextInitialized(ServletContextEvent e) {
		try (DB db = new DB()) {
			db.generateDB();
			Utils.addLog(new Log("system", System.currentTimeMillis(), "SYSTEM STARTUP"), true);
			Config config = db.getConfiguration();
			Utils.initPins(true);
			for (final PinInfo info : config.outputPins) {
				if (Utils.outputPins.containsKey(info.index)) {
					Utils.outputPins.get(info.index).setState(info.state);
					Utils.addLog(new Log("system", System.currentTimeMillis(), Log.initPin(info)), true);
					/*if (info.delay > 0) {
						new Thread(new Runnable() {
							public void run() {
								synchronized (Thread.currentThread()) {
									try {
										Thread.currentThread().wait(info.delay);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									Utils.addLog(new Log("system", System.currentTimeMillis(), Log.initPinDone(info)), true);
									Utils.outputPins.get(info.index).setState(!info.state);
								}
							}
						}).start();
					}*/
				} else {
					System.err.println("Invalid pin index " + info.index);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

}

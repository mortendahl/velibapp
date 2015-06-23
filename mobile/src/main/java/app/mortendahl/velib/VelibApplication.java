package app.mortendahl.velib;

import android.content.Context;

import app.mortendahl.velib.library.background.BaseApplication;
import app.mortendahl.velib.library.PrefHelper;
import app.mortendahl.velib.library.eventbus.EventSystem;
import app.mortendahl.velib.network.jcdecaux.Position;
import app.mortendahl.velib.network.jcdecaux.VelibStation;
import app.mortendahl.velib.network.ServerConnection;
import app.mortendahl.velib.service.MonitoredVelibStationsChangedEvent;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class VelibApplication extends BaseApplication {

	public static Position POSITION_WORK = new Position(48.8672898, 2.3520185);
	public static Position POSITION_GYM = new Position(48.866944, 2.366344);

	public static class SessionData {
		public LinkedHashMap<Integer, VelibStation> stationsMap = new LinkedHashMap<>();
	}

	private static final SessionData sessionData = new SessionData();

	public static SessionData getSessionStore() {
		return sessionData;
	}





	public static LinkedHashSet<Integer> monitoredVelibStation = new LinkedHashSet<>();

	public static void addMonitoredStation(int station) {
		monitoredVelibStation.add(station);
		EventSystem.post(new MonitoredVelibStationsChangedEvent());
	}

	@Override
    public void onCreate() {
		super.onCreate();

		cachedAppContext = getApplicationContext();

		// setup Crashlytics
		Fabric.with(this, new Crashlytics());

        // setup system
        PrefHelper.configure(cachedAppContext);
		EventSystem.configure(cachedAppContext);
        ServerConnection.configure(cachedAppContext);

        // log all bus events
        EventSystem.register(new EventBusDebugger());

	}

    private class EventBusDebugger {

		// using Object captures all events
		public void onEvent(Object event) {
			Logger.debug(Logger.TAG_SYSTEM, this, "onEvent, " + event.toString());
		}

    }
	
	private static Context cachedAppContext = null;
	
	public static Context getCachedAppContext() {
		return cachedAppContext;
	}

}

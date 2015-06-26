package app.mortendahl.velib;

import android.content.Context;
import android.location.Location;

import app.mortendahl.velib.library.background.BaseApplication;
import app.mortendahl.velib.library.PrefHelper;
import app.mortendahl.velib.library.contextaware.ContextAwareApplication;
import app.mortendahl.velib.library.contextaware.ContextAwareHandler;
import app.mortendahl.velib.library.contextaware.activity.ActivityEvent;
import app.mortendahl.velib.network.jcdecaux.Position;
import app.mortendahl.velib.network.jcdecaux.VelibStation;
import app.mortendahl.velib.network.ServerConnection;
import app.mortendahl.velib.service.MonitoredVelibStationsChangedEvent;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.location.DetectedActivity;

import app.mortendahl.velib.service.data.DataStore;
import app.mortendahl.velib.service.guiding.GuidingService;
import de.greenrobot.event.EventBus;
import io.fabric.sdk.android.Fabric;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class VelibApplication extends BaseApplication implements ContextAwareApplication {

	public static Position POSITION_WORK = new Position(48.8672898, 2.3520185);
	public static Position POSITION_GYM = new Position(48.866944, 2.366344);

	private ContextAwareHandler contextAwareHandler = new ContextAwareHandler() {

		@Override
		public void onActivityUpdate(DetectedActivity detectedActivity) {

			int type = detectedActivity.getType();
			int confidence = detectedActivity.getConfidence();
			if (type == DetectedActivity.ON_BICYCLE && confidence > 50) {
				// let the GuidingService handle the event since it knows best what it's currently doing
				GuidingService.bikingActivityAction.invoke(getApplicationContext());
			}

		}

		@Override
		public void onLocationUpdate(Location location) {

		}

	};

	@Override
	public ContextAwareHandler getContextAwareHandler() {
		return contextAwareHandler;
	}




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
		EventBus.getDefault().post(new MonitoredVelibStationsChangedEvent());
	}

	@Override
    public void onCreate() {
		super.onCreate();

		cachedAppContext = getApplicationContext();

		// setup Crashlytics
		Fabric.with(this, new Crashlytics());

        // setup system
        PrefHelper.configure(cachedAppContext);
		DataStore.configure(cachedAppContext);
        ServerConnection.configure(cachedAppContext);

        // log all bus events
        EventBus.getDefault().register(new EventBusDebugger());

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

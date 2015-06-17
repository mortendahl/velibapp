package app.mortendahl.velib;

import android.content.Context;

import app.mortendahl.velib.library.background.BaseApplication;
import app.mortendahl.velib.library.PrefHelper;
import app.mortendahl.velib.library.eventbus.EventStore;
import app.mortendahl.velib.library.ui.UiHelper;
import app.mortendahl.velib.network.jcdecaux.StationListRequest;
import app.mortendahl.velib.network.jcdecaux.VelibStation;
import app.mortendahl.velib.service.AsyncTaskRestRequest;
import app.mortendahl.velib.network.RestRequest;
import app.mortendahl.velib.service.RestResponseHandler;
import app.mortendahl.velib.network.ServerConnection;
import app.mortendahl.velib.service.MonitoredVelibStationsChangedEvent;
import app.mortendahl.velib.service.VelibStationUpdatedEvent;
import app.mortendahl.velib.service.VelibStationsChangedEvent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import de.greenrobot.event.EventBus;

public class VelibApplication extends BaseApplication {

	public static LinkedHashSet<Integer> monitoredVelibStation = new LinkedHashSet<Integer>();

	public static void addMonitoredStation(int station) {
		monitoredVelibStation.add(station);
		EventBus.getDefault().post(new MonitoredVelibStationsChangedEvent());
	}

	public static LinkedHashMap<Integer, VelibStation> stationsMap = new LinkedHashMap<>();

	protected static void updateStations(Collection<VelibStation> stations) {
		//stationsMap = new LinkedHashMap<>();
		boolean added = false;
		boolean triggerAlarm = false;
		for (VelibStation station : stations) {
			Object previousMapping = stationsMap.put(station.number, station);

			added = added || previousMapping == null;
			triggerAlarm = triggerAlarm || (monitoredVelibStation.contains(station.number) && station.availableStands == 0);
		}

		if (added) {
			EventBus.getDefault().post(new VelibStationsChangedEvent());
		} else {
			EventBus.getDefault().post(new VelibStationUpdatedEvent());
		}

		if (triggerAlarm) {
			UiHelper.vibrate();
		}
	}

	protected static StationListRequest request = null;

	public static void reloadStations() {

		if (request != null) { return; }

		request = new StationListRequest();

		new AsyncTaskRestRequest<>(request, new RestResponseHandler<StationListRequest.StationResponse>() {

			@Override
			public void onError(RestRequest<?> request, Exception e) {
				request = null;
			}

			@Override
			public void onResponse(StationListRequest.StationResponse response) {
				updateStations(response.stations);
				request = null;
			}

		}).execute();

	}

	@Override
    public void onCreate() {
		super.onCreate();
		cachedAppContext = getApplicationContext();

        // setup system
        PrefHelper.configure(cachedAppContext);
        EventStore.configure(cachedAppContext);
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

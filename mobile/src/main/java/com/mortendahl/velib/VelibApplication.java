package com.mortendahl.velib;

import android.app.Application;
import android.content.Context;

import com.mortendahl.velib.network.jcdecaux.StationListRequest;
import com.mortendahl.velib.network.jcdecaux.VelibStation;
import com.mortendahl.velib.service.AsyncTaskRestRequest;
import com.mortendahl.velib.network.RestRequest;
import com.mortendahl.velib.service.RestResponseHandler;
import com.mortendahl.velib.network.ServerConnection;
import com.mortendahl.velib.service.MonitoredVelibStationsChangedEvent;
import com.mortendahl.velib.service.VelibStationUpdatedEvent;
import com.mortendahl.velib.service.VelibStationsChangedEvent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import de.greenrobot.event.EventBus;

public class VelibApplication extends Application {

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
		initServiceLayer();
	}
	
	private static Context cachedAppContext = null;
	
	public static Context getCachedAppContext() {
		return cachedAppContext;
	}

	private void initServiceLayer() {
		ServerConnection.configure(getCachedAppContext());
	}
	
}

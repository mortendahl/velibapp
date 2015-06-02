package com.mortendahl.velib.ui.map;

import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;
import com.mortendahl.velib.Logger;
import com.mortendahl.velib.VelibApplication;
import com.mortendahl.velib.network.jcdecaux.VelibStation;
import com.mortendahl.velib.service.VelibStationsChangedEvent;

import de.greenrobot.event.EventBus;

public class MapsFragment extends SupportMapFragment {

    private ClusterManager<VelibStationMapItem> clusterManager;
    private EventBusListener eventBusListener = new EventBusListener();
    private MapListener mapListener = new MapListener();


    public static MapsFragment newInstance() {
        MapsFragment fragment = new MapsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public MapsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {}
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadStationMarkers();
        EventBus.getDefault().register(eventBusListener);
    }

    @Override
    public void onPause() {
        super.onResume();
        EventBus.getDefault().unregister(eventBusListener);
    }

    private class EventBusListener {

        public void onEvent(VelibStationsChangedEvent event) {
            reloadStationMarkers();
        }

    }

    private class MapListener implements GoogleMap.OnMapClickListener, ClusterManager.OnClusterItemClickListener<VelibStationMapItem> {

        @Override
        public void onMapClick(LatLng latLng) {
            Logger.debug(Logger.TAG_GUI, this, "onMapClick, " + latLng);
        }


        @Override
        public boolean onClusterItemClick(VelibStationMapItem item) {
            Logger.debug(Logger.TAG_GUI, this, "onClusterItemClick, " + item.getPosition());
            VelibApplication.addMonitoredStation(item.number);
            return false;
        }

    }

    protected void reloadStationMarkers() {

        GoogleMap map = getMap();
        if (map == null) { return; }

        if (clusterManager == null) {
            clusterManager = new ClusterManager<>(getActivity(), map);
            map.setOnCameraChangeListener(clusterManager);
            map.setOnMarkerClickListener(clusterManager);
            clusterManager.setOnClusterItemClickListener(mapListener);
        }

        map.setOnMapClickListener(mapListener);

        int count = 0;
        clusterManager.clearItems();
        for (VelibStation station : VelibApplication.stationsMap.values()) {
            clusterManager.addItem(VelibStationMapItem.fromStation(station));
            count += 1;
        }
        Logger.debug(Logger.TAG_GUI, this, "added " + count + " markers");

    }

}

package com.mortendahl.velib.ui.map;

import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.mortendahl.velib.Logger;
import com.mortendahl.velib.VelibApplication;
import com.mortendahl.velib.network.jcdecaux.Position;
import com.mortendahl.velib.network.jcdecaux.VelibStation;
import com.mortendahl.velib.service.GuidingService;
import com.mortendahl.velib.service.VelibStationsChangedEvent;

import java.util.Collection;

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

    protected boolean zoomMapToMarkers = true;
    protected Marker currentDestination = null;

    private class MapListener implements GoogleMap.OnMapClickListener, ClusterManager.OnClusterItemClickListener<VelibStationMapItem>, GoogleMap.OnMapLongClickListener {

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



        @Override
        public void onMapLongClick(LatLng latlng) {
            Logger.debug(Logger.TAG_GUI, this, "onMapLongClick, " + latlng);

            if (currentDestination == null) {
                currentDestination = getMap().addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        .position(latlng)
                );
            } else {
                currentDestination.setPosition(latlng);
            }

            GuidingService.setDestinationAction.invoke(getActivity(), new Position(latlng.latitude, latlng.longitude));

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

            map.setOnMapLongClickListener(mapListener);
            map.setOnMapClickListener(mapListener);
        }

        clusterManager.clearItems();

        Collection<VelibStation> stations = VelibApplication.stationsMap.values();
        if (stations.isEmpty()) { return; }

        for (VelibStation station : stations) {
            clusterManager.addItem(VelibStationMapItem.fromStation(station));
        }

        if (zoomMapToMarkers) {

            zoomMapToMarkers = false;

            LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
            for (VelibStation station : stations) {
                boundsBuilder.include(new LatLng(station.position.latitude, station.position.longitude));
            }
            LatLngBounds bounds = boundsBuilder.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
        }

    }

}

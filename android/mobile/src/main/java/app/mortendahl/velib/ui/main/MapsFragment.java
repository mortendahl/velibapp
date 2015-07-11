package app.mortendahl.velib.ui.main;

import android.graphics.Bitmap;
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
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import app.mortendahl.velib.Logger;
import app.mortendahl.velib.R;
import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.library.ui.BitmapHelper;
import app.mortendahl.velib.network.jcdecaux.VelibStation;
import app.mortendahl.velib.service.guiding.GuidingService;
import app.mortendahl.velib.service.stationupdator.StationUpdatorService;
import app.mortendahl.velib.service.stationupdator.VelibStationUpdatedEvent;
import app.mortendahl.velib.service.stationupdator.VelibStationsChangedEvent;
import de.greenrobot.event.EventBus;

import java.util.Collection;

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
        StationUpdatorService.updatesAction.request(getActivity(), getClass().getSimpleName());
        EventBus.getDefault().register(eventBusListener);
    }

    @Override
    public void onPause() {
        super.onResume();
        StationUpdatorService.updatesAction.remove(getActivity(), getClass().getSimpleName());
        EventBus.getDefault().unregister(eventBusListener);
    }

    private class EventBusListener {

        public void onEvent(VelibStationsChangedEvent event) {
            reloadStationMarkers();
        }

        public void onEvent(VelibStationUpdatedEvent event) {
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

            GuidingService.setDestinationAction.invoke(getActivity(), latlng.latitude, latlng.longitude);

        }
    }

    private class VelibStationRenderer extends DefaultClusterRenderer<VelibStationMapItem> {

        public final BitmapDescriptor plentyStandsAvailableStationIcon;
        public final BitmapDescriptor fewStandsAvailableStationIcon;
        public final BitmapDescriptor noneStandsAvailableStationIcon;
        public final BitmapDescriptor closedStationIcon;

        public VelibStationRenderer(GoogleMap map, ClusterManager clusterManager) {
            super(getActivity(), map, clusterManager);

            plentyStandsAvailableStationIcon = createStationIcon(getResources().getColor(R.color.station_icon_stands_available_plenty));
            fewStandsAvailableStationIcon = createStationIcon(getResources().getColor(R.color.station_icon_stands_available_few));
            noneStandsAvailableStationIcon = createStationIcon(getResources().getColor(R.color.station_icon_stands_available_none));
            closedStationIcon = createStationIcon(getResources().getColor(R.color.station_icon_closed));
        }

        private BitmapDescriptor createStationIcon(int color) {
            Bitmap icon;
            //icon = BitmapHelper.createTextBitmap(100, color, text);
            icon = BitmapHelper.createSolidBitmap(100, color);
            icon = BitmapHelper.cropBitmapToCircle(icon);
            return BitmapDescriptorFactory.fromBitmap(icon);
        }

        @Override
        protected void onBeforeClusterItemRendered(VelibStationMapItem mapItem, MarkerOptions markerOptions) {

            // doesnt not refresh properly; seems we really need to re-add all markers
            //VelibStation velibStation = VelibApplication.stationsMap.get(mapItem.number);
            //int availableStands = (velibStation != null ? velibStation.availableStands : -1);

            //String text = String.format("%d", mapItem.availableStands);

            BitmapDescriptor icon;
            if (!mapItem.open) {
                icon = closedStationIcon;
            } else if (mapItem.availableStands <= 0) {
                icon = noneStandsAvailableStationIcon;
            } else if (mapItem.availableStands <= 5) {
                icon = fewStandsAvailableStationIcon;
            } else {
                icon = plentyStandsAvailableStationIcon;
            }

            markerOptions
                    .icon(icon)
                    .title(mapItem.name + "\n" + mapItem.availableStands);
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster<VelibStationMapItem> cluster) {
            return cluster.getSize() >= 4;
        }

    }

    protected VelibStationRenderer renderer;

    private long previousReloadTimestamp = 0;

    protected void reloadStationMarkers() {

        GoogleMap map = getMap();
        if (map == null) { return; }

        long currentTimestamp = System.currentTimeMillis();

        if (clusterManager == null) {
            clusterManager = new ClusterManager<>(getActivity(), map);
            renderer = new VelibStationRenderer(map, clusterManager);
            clusterManager.setRenderer(renderer);
            map.setOnCameraChangeListener(clusterManager);
            map.setOnMarkerClickListener(clusterManager);
            clusterManager.setOnClusterItemClickListener(mapListener);
            map.setOnMapLongClickListener(mapListener);
            map.setOnMapClickListener(mapListener);
        } else {

            if (currentTimestamp - previousReloadTimestamp < 10000) {
                Logger.debug(Logger.TAG_GUI, this, "reloadStationMarkers, skipping");
                return;
            }

        }


        Logger.debug(Logger.TAG_GUI, this, "reloadStationMarkers");

        clusterManager.clearItems();
        Collection<VelibStation> stations = VelibApplication.getSessionStore().stationsMap.values();
        if (stations.isEmpty()) { return; }

        previousReloadTimestamp = currentTimestamp;

        for (VelibStation station : stations) {
            clusterManager.addItem(VelibStationMapItem.fromStation(station));
        }
        // force refresh
        clusterManager.cluster();



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

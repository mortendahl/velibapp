package app.mortendahl.velib;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

//public class MainActivity extends FragmentActivity {
public class MainActivity extends WearableActivity {

    protected final String TAG = "VelibWear";

    protected GoogleMap googleMap;
    private GoogleApiClient googleApiClient;
    private GoogleApiClientCallbacks googleApiClientCallbacks = new GoogleApiClientCallbacks();
    private GoogleMapsCallbacks googleMapsCallbacks = new GoogleMapsCallbacks();
    private GoogleLocationListener googleLocationListener = new GoogleLocationListener();
    private GoogleDataMapListener googleDataMapListener = new GoogleDataMapListener();

//    private DismissOverlayView mDismissOverlay;

    private View ambientView;
    private TextView ambientNameTextView;
    private TextView ambientStandsTextView;

    private View nostationView;

    /*
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
    }
    */

    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setAmbientEnabled();

        // Set the layout. It only contains a SupportMapFragment and a DismissOverlay.
        setContentView(R.layout.activity_main);

        // Retrieve the containers for the root of the layout and the map. Margins will need to be
        // set on them to account for the system window insets.
        final FrameLayout topFrameLayout = (FrameLayout) findViewById(R.id.root_container);
        final FrameLayout mapFrameLayout = (FrameLayout) findViewById(R.id.map_container);

        // Set the system view insets on the containers when they become available.
        topFrameLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                // Call through to super implementation and apply insets
                insets = topFrameLayout.onApplyWindowInsets(insets);

                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) mapFrameLayout.getLayoutParams();

                // Add Wearable insets to FrameLayout container holding map as margins
                params.setMargins(
                        insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom());
                mapFrameLayout.setLayoutParams(params);

                return insets;
            }
        });

//        // Obtain the DismissOverlayView and display the intro help text.
//        mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
//        mDismissOverlay.setIntroText(R.string.intro_text);
//        mDismissOverlay.showIntroIfNecessary();

        nostationView = findViewById(R.id.nostation);

        ambientView = findViewById(R.id.ambient);
        ambientNameTextView = (TextView) findViewById(R.id.ambient_stationname);
        ambientStandsTextView = (TextView) findViewById(R.id.ambient_stands);

        // Obtain the MapFragment and set the async listener to be notified when the map is ready.
        //SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(googleMapsCallbacks);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)  // used for data layer API
                .addConnectionCallbacks(googleApiClientCallbacks)
                .addOnConnectionFailedListener(googleApiClientCallbacks)
                .build();

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!googleApiClient.isConnected()) { googleApiClient.connect(); }

        nostationView.setVisibility(station == null ? View.VISIBLE : View.GONE);
        ambientView.setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (googleApiClient.isConnected()) { LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, googleLocationListener); }
        if (googleApiClient.isConnected()) { Wearable.DataApi.removeListener(googleApiClient, googleDataMapListener); }
        googleApiClient.disconnect();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        Log.d(TAG, "onEnterAmbient");

        ambientView.setVisibility(View.VISIBLE);

        ambientNameTextView.setText(station != null ? station.name : "--");
        ambientStandsTextView.setText(station != null ? "" + station.stands : "--");
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        Log.d(TAG, "onUpdateAmbient");

        ambientNameTextView.setText(station != null ? station.name : "--");
        ambientStandsTextView.setText(station != null ? "" + station.stands : "--");
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        Log.d(TAG, "onExitAmbient");

        ambientView.setVisibility(View.GONE);
    }

    protected LatLng ownLatLng = null;
    protected Marker ownMarker = null;

    protected void refreshOwnMarker() {

//        if (googleMap == null || ownLatLng == null) { return; }
//
//        Log.d(TAG, "updating own marker");
//
//        if (ownMarker == null) {
//            ownMarker = googleMap.addMarker(new MarkerOptions()
//                    .title("You")
//                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
//                    .position(ownLatLng));
//        } else {
//            ownMarker.setPosition(ownLatLng);
//        }

    }

    protected Station station = null;
    protected Marker stationMarker = null;

    protected void setStation(@Nullable Station station) {
        this.station = station;
        nostationView.setVisibility(station == null ? View.VISIBLE : View.GONE);
        refreshStationMarker();
        updateCameraPosition();
    }

    protected Station extractStationFromDataMap(DataItem dataItem) {

        DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
        Station station = new Station();

        station.name = dataMap.getString("best_dest_name");
        station.stands = dataMap.getInt("best_dest_stands");
        double latitude = dataMap.getDouble("best_dest_latitude");
        double longitude = dataMap.getDouble("best_dest_longitude");
        station.latlng = new LatLng(latitude, longitude);

        return station;

    }

    protected void refreshStationMarker() {

        if (googleMap == null) { return; }

        if (station == null) {

            Log.d(TAG, "deleting station marker, " + stationMarker);

            if (stationMarker != null) {
                stationMarker.remove();
                stationMarker = null;

                // should not be needed, but sometimes the map is not updating properly so let's just try various things
                googleMap.clear();
            }

        } else {

            Log.d(TAG, "updating station marker, " + station);

            if (stationMarker == null) {
                stationMarker = googleMap.addMarker(new MarkerOptions()
                        .title("Station")
                        .position(station.latlng));
            } else {
                stationMarker.setPosition(station.latlng);
            }

            if (station.stands == 0) {
                stationMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            } else if (station.stands <= 5) {
                stationMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            } else {
                stationMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            }

        }

    }

    private void updateCameraPosition() {

//        if (ownLatLng != null || stationLatLng != null) {
//            LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
//            //for (VelibStation station : stations) {
//            if (ownLatLng != null) {
//                boundsBuilder.include(ownLatLng);
//            }
//            if (stationLatLng != null) {
//                boundsBuilder.include(stationLatLng);
//            }
//            //}
//            LatLngBounds bounds = boundsBuilder.build();
//            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
//        }

        Log.d(TAG, "updating camera position, " + station);

        if (station == null) { return; }

//        LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
//        boundsBuilder.include(stationLatLng);
//        boundsBuilder.include(new LatLng(stationLatLng.latitude + .001f, stationLatLng.longitude + .001f));
//        boundsBuilder.include(new LatLng(stationLatLng.latitude - .001f, stationLatLng.longitude - .001f));
//        LatLngBounds bounds = boundsBuilder.build();
//        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(station.latlng, 16));

    }

    private class GoogleApiClientCallbacks implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnected(Bundle bundle) {

            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(2000)
                    .setMaxWaitTime(2 * 2000)
                    .setFastestInterval(2000);

            LocationServices.FusedLocationApi
                    .requestLocationUpdates(googleApiClient, locationRequest, googleLocationListener)
                    .setResultCallback(new ResultCallback<Status>() {

                        @Override
                        public void onResult(Status result) {

                        }

//                        @Override
//                        public void onResult(Status status) {
//                            if (status.getStatus().isSuccess()) {
//                                if (Log.isLoggable(TAG, Log.DEBUG)) {
//                                    Log.d(TAG, "Successfully requested location updates");
//                                }
//                            } else {
//                                Log.e(TAG,
//                                        "Failed in requesting location updates, "
//                                                + "status code: "
//                                                + status.getStatusCode()
//                                                + ", message: "
//                                                + status.getStatusMessage());
//                            }
//                        }
                    });

            Log.d(TAG, "adding dataapi hooks");
            Wearable.DataApi.addListener(googleApiClient, googleDataMapListener);
            Wearable.DataApi.getDataItems(googleApiClient)
                    .setResultCallback(new ResultCallback<DataItemBuffer>() {
                        @Override
                        public void onResult(DataItemBuffer dataItems) {
                            Log.d(TAG, "onResult, " + dataItems.getCount());
                            Station station = null;
                            for (DataItem dataItem : dataItems) {
                                String path = dataItem.getUri().getPath();
                                Log.d(TAG, "DataItem, " + path);
                                if ("/best_dest".equals(path)) {
                                    station = extractStationFromDataMap(dataItem);
                                }
                            }
                            setStation(station);
                        }
                    });

        }

        @Override
        public void onConnectionSuspended(int i) {}

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {}

    }

    private class GoogleDataMapListener implements DataApi.DataListener {

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
            dataEvents.close();

            Log.d(TAG, "onDataChanged");

            Station station = null;
            for (DataEvent event : events) {
                DataItem dataItem = event.getDataItem();
                String path = dataItem.getUri().getPath();
                Log.d(TAG, "DataItem, " + path);
                int type = event.getType();
                if ("/best_dest".equals(path)) {
                    if (type == DataEvent.TYPE_CHANGED) {
                        DataItem item = event.getDataItem();
                        station = extractStationFromDataMap(item);
                    } else if (type == DataEvent.TYPE_DELETED) {
                        station = null;
                    }
                }
            }
            setStation(station);
        }

    }

    private class GoogleLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
//            ownLatLng = new LatLng(location.getLatitude(), location.getLongitude());
//            refreshOwnMarker();
//            updateCameraPosition();
        }

    }

    private class GoogleMapsCallbacks implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

        @Override
        public void onMapReady(GoogleMap map) {

            // Map is ready to be used.
            googleMap = map;

            // Set the long click listener as a way to exit the map.
            googleMap.setOnMapLongClickListener(this);

            refreshOwnMarker();
            refreshStationMarker();
            updateCameraPosition();

        }

        @Override
        public void onMapLongClick(LatLng latLng) {
            updateCameraPosition();
        }

    }

}

package app.mortendahl.velib;

import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DismissOverlayView;
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
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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

        ambientNameTextView.setText(stationName != null ? stationName : "--");
        ambientStandsTextView.setText(stationStands != null ? stationStands.toString() : "--");
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        Log.d(TAG, "onUpdateAmbient");

        ambientNameTextView.setText(stationName != null ? stationName : "--");
        ambientStandsTextView.setText(stationStands != null ? stationStands.toString() : "--");
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        Log.d(TAG, "onExitAmbient");

        ambientView.setVisibility(View.GONE);
    }

    protected LatLng ownLatLng = null;
    protected Marker ownPin = null;

    protected String stationName = null;
    protected Integer stationStands = null;
    protected LatLng stationLatLng = null;
    protected Marker stationPin = null;

    protected void refreshOwnMarker() {

//        if (googleMap == null || ownLatLng == null) { return; }
//
//        Log.d(TAG, "updating own marker");
//
//        if (ownPin == null) {
//            ownPin = googleMap.addMarker(new MarkerOptions()
//                    .title("You")
//                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
//                    .position(ownLatLng));
//        } else {
//            ownPin.setPosition(ownLatLng);
//        }
//
//        updateCameraPosition();

    }

    protected void updateStationMarkerFromDataItem(DataItem dataItem) {
        DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
        stationName = dataMap.getString("best_dest_name");
        stationStands = dataMap.getInt("best_dest_stands");
        double latitude = dataMap.getDouble("best_dest_latitude");
        double longitude = dataMap.getDouble("best_dest_longitude");
        stationLatLng = new LatLng(latitude, longitude);
        refreshStationMarker();
    }

    protected void refreshStationMarker() {

        Log.d(TAG, "updating station marker, " + stationName + ", " + stationLatLng);

        if (googleMap == null) { return; }
        if (stationLatLng == null) { return; }

        if (stationPin == null) {
            stationPin = googleMap.addMarker(new MarkerOptions()
                    .title("Station")
                    //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .position(stationLatLng));
        } else {
            stationPin.setPosition(stationLatLng);
        }

        if (stationStands == 0) {
            stationPin.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        } else if (stationStands <= 5) {
            stationPin.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
        } else {
            stationPin.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        }

        updateCameraPosition();

    }

    protected void updateCameraPosition() {

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

        if (stationLatLng == null) { return; }

//        LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
//        boundsBuilder.include(stationLatLng);
//        boundsBuilder.include(new LatLng(stationLatLng.latitude + .001f, stationLatLng.longitude + .001f));
//        boundsBuilder.include(new LatLng(stationLatLng.latitude - .001f, stationLatLng.longitude - .001f));
//        LatLngBounds bounds = boundsBuilder.build();
//        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stationLatLng, 17));

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
                            for (DataItem dataItem : dataItems) {
                                String path = dataItem.getUri().getPath();
                                Log.d(TAG, "DataItem, " + path);
                                if ("/best_dest".equals(path)) {
                                    updateStationMarkerFromDataItem(dataItem);
                                }
                            }
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

            for (DataEvent event : events) {
                DataItem dataItem = event.getDataItem();
                String path = dataItem.getUri().getPath();
                Log.d(TAG, "DataItem, " + path);
                int type = event.getType();
                if ("/best_dest".equals(path) && type == DataEvent.TYPE_CHANGED) {
                    DataItem item = event.getDataItem();
                    updateStationMarkerFromDataItem(item);
                }
            }
        }

    }

    private class GoogleLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            ownLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            refreshOwnMarker();
        }

    }

    private class GoogleMapsCallbacks implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

        @Override
        public void onMapReady(GoogleMap map) {

            // Map is ready to be used.
            googleMap = map;

            // Set the long click listener as a way to exit the map.
            googleMap.setOnMapLongClickListener(this);

//            // Add a marker with a title that is shown in its info window.
//            mMap.addMarker(new MarkerOptions().position(SYDNEY)
//                    .title("Sydney Opera House"));
//
//            // Move the camera to show the marker.
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SYDNEY, 10));

            refreshOwnMarker();
            refreshStationMarker();

        }

        @Override
        public void onMapLongClick(LatLng latLng) {
            // Display the dismiss overlay with a button to exit this activity.
//            mDismissOverlay.show();
        }

    }

}

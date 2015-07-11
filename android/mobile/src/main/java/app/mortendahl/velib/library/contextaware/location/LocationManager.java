package app.mortendahl.velib.library.contextaware.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import app.mortendahl.velib.library.background.BaseIntentService;
import app.mortendahl.velib.library.background.IntentServiceActionHandler;

import java.util.concurrent.TimeUnit;

public class LocationManager extends BaseIntentService {

    public static final SetFrequencyActionHandler.Invoker frequencyAction = new SetFrequencyActionHandler.Invoker();

    public LocationManager() {
        setActionHandlers(
                new SetFrequencyActionHandler(this)
        );
    }

    protected class GoogleApiClientCallbacks implements GoogleApiClient.ConnectionCallbacks,
                                                        GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnectionSuspended(int cause) {

            // TODO do we still receive updates?

//				if (cause == ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
//
//				} else if (cause == ConnectionCallbacks.CAUSE_NETWORK_LOST) {
//
//				}

        }

        @Override
        public void onConnected(Bundle bundle) {


        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {

            // at least one of the API client connect attempts failed
            //  - no client is connected (according to Android Developers Blog)

            // TODO do we still receive updates?

        }

    }

    protected GoogleApiClientCallbacks googleApiClientCallbacks;
    protected GoogleApiClient googleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();

        googleApiClientCallbacks = new GoogleApiClientCallbacks();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(googleApiClientCallbacks)
                .addOnConnectionFailedListener(googleApiClientCallbacks)
                .build();
    }

    public static class SetFrequencyActionHandler extends IntentServiceActionHandler {

        protected static final String ACTION = "set_frequency";
        protected static final String KEY_INTERVAL = "frequency";

        @Override
        public String getAction() {
            return ACTION;
        }

        public static class Invoker {

            private void setInterval(Context context, int intervalInSeconds) {
                Intent intent = new Intent(context, LocationManager.class);
                intent.setAction(ACTION);
                intent.putExtra(KEY_INTERVAL, intervalInSeconds);
                context.startService(intent);
            }

            public void turnHigh(Context context) {
                setInterval(context, 10);
            }

            public void turnLow(Context context) {
                setInterval(context, 5 * 60);
            }

            public void turnOff(Context context) {
                setInterval(context, Integer.MAX_VALUE);
            }

        }

        protected final LocationManager state;

        public SetFrequencyActionHandler(LocationManager locationManager) {
            this.state = locationManager;
        }

        @Override
        public void handle(Context context, Intent intent) {

            // either no-op if already connected or wait until new connection established
            state.googleApiClient.blockingConnect(10000, TimeUnit.MILLISECONDS);

            int intervalInSeconds = intent.getIntExtra(KEY_INTERVAL, 0);

            // todo checks could be improved
            if (!state.googleApiClient.isConnected()) { return; }
            if (intervalInSeconds == 0) { return; }

            PendingIntent pendingIntent = LocationReceiver.LocationUpdateHandler.getPendingIntent(context);
            Status status;

            if (0 < intervalInSeconds && intervalInSeconds < Integer.MAX_VALUE) {

                //
                // request updates
                //

                LocationRequest locationRequest = LocationRequest.create()
                        .setInterval(intervalInSeconds * 1000)
                        .setFastestInterval(3000)
                        .setMaxWaitTime(2 * intervalInSeconds)  // recommended by Google
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setSmallestDisplacement(0);  // in meters

                status = LocationServices.FusedLocationApi
                        .requestLocationUpdates(state.googleApiClient, locationRequest, pendingIntent)
                        .await(10000, TimeUnit.MILLISECONDS);

            } else {

                //
                // remove updates
                //

                status = LocationServices.FusedLocationApi
                        .removeLocationUpdates(state.googleApiClient, pendingIntent)
                        .await(10000, TimeUnit.MILLISECONDS);

            }

            if (status.isSuccess()) {

                // TODO

            }

            // TODO we could disconnect here
            //state.googleApiClient.disconnect();

        }

    }



}

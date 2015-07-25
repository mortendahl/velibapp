package app.mortendahl.velib.library.contextaware.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import app.mortendahl.velib.library.PrefHelper;
import app.mortendahl.velib.library.background.BaseIntentService;
import app.mortendahl.velib.library.background.IntentServiceActionHandler;
import io.fabric.sdk.android.Fabric;

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
        protected static final String PREFKEY_LEVEL = "location_update_level";

        private static final int LEVEL_OFF =    0;
        private static final int LEVEL_LOW =    1;
        private static final int LEVEL_HIGH =   2;

        @Override
        public String getAction() {
            return ACTION;
        }

        public static class Invoker {

            public void reload(Context context) {
                Intent intent = new Intent(context, LocationManager.class);
                intent.setAction(ACTION);
                context.startService(intent);
            }

            public void turnActive(Context context) {
                PrefHelper.saveInteger(PREFKEY_LEVEL, LEVEL_HIGH);
                reload(context);
            }

            public void turnPassive(Context context) {
                PrefHelper.saveInteger(PREFKEY_LEVEL, LEVEL_LOW);  // alternatively LEVEL_OFF
                reload(context);
            }

            public void turnOff(Context context) {
                PrefHelper.saveInteger(PREFKEY_LEVEL, LEVEL_OFF);
                reload(context);
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

            if (!state.googleApiClient.isConnected()) {
                CrashlyticsCore.getInstance().logException(new Exception("google client did not connect, ignoring request"));
                return;
            }

            int level = PrefHelper.loadInteger(PREFKEY_LEVEL, LEVEL_OFF);  // default is OFF
            LocationRequest locationRequest = buildLocationRequest(level);

            PendingIntent pendingIntent = LocationReceiver.LocationUpdateHandler.getPendingIntent(context);
            Status status;
            if (locationRequest != null) {

                // request updates
                status = LocationServices.FusedLocationApi
                        .requestLocationUpdates(state.googleApiClient, locationRequest, pendingIntent)
                        .await(10000, TimeUnit.MILLISECONDS);

            } else {

                // remove updates
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

        /**
         * Returning null indicates that we don't want any location updates.
         */
        private LocationRequest buildLocationRequest(int level) {

            if (level == LEVEL_LOW) {

                int intervalInSeconds = 5 * 60;     // 5 min
                int fastestIntervalInSeconds = 60;  // 1 min

                LocationRequest locationRequest = LocationRequest.create()
                        .setInterval(intervalInSeconds * 1000)
                        .setFastestInterval(fastestIntervalInSeconds * 1000)
                        .setMaxWaitTime(2 * intervalInSeconds)  // recommended by Google
                        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                        .setSmallestDisplacement(50);  // in meters

                return locationRequest;

            } else if (level == LEVEL_HIGH) {

                int intervalInSeconds = 10;
                int fastestIntervalInSeconds = 5;

                LocationRequest locationRequest = LocationRequest.create()
                        .setInterval(intervalInSeconds * 1000)
                        .setFastestInterval(fastestIntervalInSeconds * 1000)
                        .setMaxWaitTime(2 * intervalInSeconds)  // recommended by Google
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setSmallestDisplacement(0);  // in meters

                return locationRequest;

            } else {  // including LEVEL_LOW

                return null;

            }

        }

    }

}

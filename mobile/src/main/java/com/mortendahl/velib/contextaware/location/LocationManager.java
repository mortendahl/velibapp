package com.mortendahl.velib.contextaware.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.mortendahl.velib.library.*;

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

    public static class SetFrequencyActionHandler extends ActionHandler {

        protected static final String ACTION = "set_frequency";
        protected static final String KEY_INTERVAL = "frequency";

        @Override
        public String getAction() {
            return ACTION;
        }

        public static class Invoker {
            public void setInterval(Context context, int interval) {
                Intent intent = new Intent(context, LocationManager.class);
                intent.setAction(ACTION);
                intent.putExtra(KEY_INTERVAL, interval);
                context.startService(intent);
            }

            public void turnOff(Context context) {
                setInterval(context, -1);
            }
        }

        protected final LocationManager state;

        public SetFrequencyActionHandler(LocationManager locationManager) {
            this.state = locationManager;
        }

        @Override
        public void handle(Context context, Intent intent) {

            state.googleApiClient.blockingConnect(10000, TimeUnit.MILLISECONDS);

            int interval = intent.getIntExtra(KEY_INTERVAL, 0);

            // todo checks could be improved
            if (!state.googleApiClient.isConnected()) { return; }
            if (interval == 0) { return; }

            PendingIntent pendingIntent = LocationReceiver.getLocationUpdatePendingIntent(context);
            Status status;

            if (interval > 0) {

                //
                // request updates
                //

                LocationRequest locationRequest = LocationRequest.create()
                        .setInterval(interval)
                        .setFastestInterval(3000)
                        .setMaxWaitTime(2 * interval)
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

            }

        }

    }



}

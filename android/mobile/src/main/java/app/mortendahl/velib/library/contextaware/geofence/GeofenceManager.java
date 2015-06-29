package app.mortendahl.velib.library.contextaware.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.TimeUnit;

import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.library.background.BaseIntentService;
import app.mortendahl.velib.library.background.IntentServiceActionHandler;
import app.mortendahl.velib.network.jcdecaux.Position;

public class GeofenceManager extends BaseIntentService {

    public static final RefreshFencesActionHandler.Invoker refreshFencesAction = new RefreshFencesActionHandler.Invoker();

    public GeofenceManager() {
        setActionHandlers(
                new RefreshFencesActionHandler(this)
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
        public void onConnected(Bundle bundle) {}

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

    public static class RefreshFencesActionHandler extends IntentServiceActionHandler {

        protected static final String ACTION = "refresh_fences";

        @Override
        public String getAction() {
            return ACTION;
        }

        public static class Invoker {
            public void invoke(Context context) {
                //
                // TODO should take a wake lock -- wakefulbroadcastreceiver?
                //
                Intent intent = new Intent(context, GeofenceManager.class);
                intent.setAction(ACTION);
                context.startService(intent);
            }
        }

        protected final GeofenceManager state;

        public RefreshFencesActionHandler(GeofenceManager locationManager) {
            this.state = locationManager;
        }

        @Override
        public void handle(Context context, Intent intent) {

            state.googleApiClient.blockingConnect(10000, TimeUnit.MILLISECONDS);

            final int radius = 100;  // min recommended by Google

            Position workPosition = VelibApplication.POSITION_WORK;
            installGeofence(context, "work", workPosition.latitude, workPosition.longitude, radius);

        }

        private boolean installGeofence(Context context, String fenceId, double latitude, double longitude, int radius) {

            PendingIntent pendingIntent = GeofenceReceiver.GeofenceTransitionHandler.getPendingIntent(context);

            Geofence geofence = new Geofence.Builder()
                    .setRequestId(fenceId)
                    .setCircularRegion(latitude, longitude, radius)  // radius in meters
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)  // in miliseconds
                    .setLoiteringDelay(10 * 1000)  // in miliseconds
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            GeofencingRequest request = new GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build();

            Status status = LocationServices.GeofencingApi
                    .addGeofences(state.googleApiClient, request, pendingIntent)
                    .await(10000, TimeUnit.MILLISECONDS);

            return status.isSuccess();

        }

    }

}

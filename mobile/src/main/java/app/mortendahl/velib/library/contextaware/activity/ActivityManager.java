package app.mortendahl.velib.library.contextaware.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;

import java.util.concurrent.TimeUnit;

import app.mortendahl.velib.Logger;
import app.mortendahl.velib.library.PrefHelper;
import app.mortendahl.velib.library.background.ActionHandler;
import app.mortendahl.velib.library.background.BaseIntentService;

public class ActivityManager extends BaseIntentService {

    public static final FrequencyActionHandler.Invoker frequencyAction = new FrequencyActionHandler.Invoker();

    public ActivityManager() {
        setActionHandlers(
                new FrequencyActionHandler.Handler(this)
        );
    }

    protected class GoogleApiClientCallbacks implements GoogleApiClient.ConnectionCallbacks,
                                                        GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnectionSuspended(int cause) {
            // TODO need to do anything?
        }

        @Override
        public void onConnected(Bundle bundle) {}

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            // TODO need to do anything?
        }

    }

    protected GoogleApiClientCallbacks googleApiClientCallbacks;
    protected GoogleApiClient googleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();

        googleApiClientCallbacks = new GoogleApiClientCallbacks();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(googleApiClientCallbacks)
                .addOnConnectionFailedListener(googleApiClientCallbacks)
                .build();
    }

    public static class FrequencyActionHandler {

        protected static final String ACTION = "set_frequency";
        protected static final String PREFKEY_INTERVAL = "activity_update_frequency";

        public static class Invoker {

            public void refresh(Context context) {
                Intent intent = new Intent(context, ActivityManager.class);
                intent.setAction(ACTION);
                context.startService(intent);
            }

            public void setInterval(Context context, int intervalInSeconds) {
                PrefHelper.saveInteger(PREFKEY_INTERVAL, intervalInSeconds);
                refresh(context);
            }

            public void turnOff(Context context) {
                setInterval(context, -1);
            }

        }

        public static class Handler extends ActionHandler {

            protected final ActivityManager state;

            public Handler(ActivityManager locationManager) {
                this.state = locationManager;
            }

            @Override
            public void handle(Context context, Intent intent) {

                state.googleApiClient.blockingConnect(10000, TimeUnit.MILLISECONDS);

                int intervalInSeconds = PrefHelper.loadInteger(PREFKEY_INTERVAL, -1);  // defaults to removing updates

                // todo checks could be improved
                if (!state.googleApiClient.isConnected()) { return; }
                if (intervalInSeconds == 0) { return; }

                PendingIntent pendingIntent = ActivityReceiver.ActivityUpdateHandler.getPendingIntent(context);
                Status status;

                if (intervalInSeconds > 0) {

                    //
                    // request updates
                    //

                    status = ActivityRecognition.ActivityRecognitionApi
                            .requestActivityUpdates(state.googleApiClient, intervalInSeconds * 1000, pendingIntent)
                            .await(10000, TimeUnit.MILLISECONDS);

                    Logger.debug(Logger.TAG_SERVICE, this, "requested updates, " + intervalInSeconds);

                } else {

                    //
                    // remove updates
                    //

                    status = ActivityRecognition.ActivityRecognitionApi
                            .removeActivityUpdates(state.googleApiClient, pendingIntent)
                            .await(10000, TimeUnit.MILLISECONDS);

                    Logger.debug(Logger.TAG_SERVICE, this, "removed updates");

                }

                if (status.isSuccess()) {

                    // TODO

                }

                // TODO we could disconnect here
                //state.googleApiClient.disconnect();

            }

            @Override
            public String getAction() {
                return ACTION;
            }

        }

    }

}

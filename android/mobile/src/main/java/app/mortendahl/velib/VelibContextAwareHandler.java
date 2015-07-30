package app.mortendahl.velib;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.location.DetectedActivity;

import app.mortendahl.velib.library.contextaware.BaseEvent;
import app.mortendahl.velib.library.contextaware.ContextAwareHandler;
import app.mortendahl.velib.library.contextaware.activity.ActivityUpdateEvent;
import app.mortendahl.velib.library.contextaware.connectivity.ConnectivityChangeEvent;
import app.mortendahl.velib.library.contextaware.connectivity.ConnectivityStabilisedEvent;
import app.mortendahl.velib.library.contextaware.geofence.GeofenceTransitionEvent;
import app.mortendahl.velib.library.contextaware.location.LocationUpdateEvent;
import app.mortendahl.velib.library.contextaware.power.PowerUpdateEvent;
import app.mortendahl.velib.service.data.DataStore;
import app.mortendahl.velib.service.guiding.GuidingService;
import app.mortendahl.velib.ui.main.MainActivity;
import de.greenrobot.event.EventBus;

public class VelibContextAwareHandler implements ContextAwareHandler {

    public static final String eventStoreId = "eventstore";

    private void record(BaseEvent event) {
        DataStore.getCollection(eventStoreId).append(event);
    }

    private void post(BaseEvent event) {
        EventBus.getDefault().post(event);
    }

    @Override
    public void onPowerUpdate(PowerUpdateEvent event) {
        record(event);
        post(event);
    }

    @Override
    public void onActivityUpdate(ActivityUpdateEvent event) {

        record(event);
        post(event);

        int onBicycleConfidence = event.getConfidence(DetectedActivity.ON_BICYCLE);
        if (onBicycleConfidence > 50) {
            // let the GuidingService handle the event since it knows best what it's currently doing
            Context context = VelibApplication.getCachedAppContext();
            GuidingService.bikingActivityAction.invoke(context);
        }

    }

    @Override
    public void onLocationUpdate(LocationUpdateEvent event) {
        record(event);
        post(event);
    }

    @Override
    public void onGeofenceTransition(GeofenceTransitionEvent event) {

        record(event);
        post(event);

        Context context = VelibApplication.getCachedAppContext();

        String fenceId = event.fenceIds.size() > 0 ? event.fenceIds.get(0) : "--";
        Notification transitionNotification = buildGeofenceTransitionNotification(context, fenceId, GeofenceTransitionEvent.describeTransition(event.transition));
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(303, transitionNotification);

    }

    @Override
    public void onConnectivityChange(ConnectivityChangeEvent event) {
        //record(event);
    }

    @Override
    public void onConnectivityStabilised(ConnectivityStabilisedEvent event) {
        record(event);
        post(event);
    }

    private Notification buildGeofenceTransitionNotification(Context context, String fenceId, String transition) {

        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(fenceId + ", " + transition)
                .setContentText("Geofence")
                .setContentIntent(viewPendingIntent);

        return notificationBuilder.build();

    }

}

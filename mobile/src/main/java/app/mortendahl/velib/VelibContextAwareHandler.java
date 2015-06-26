package app.mortendahl.velib;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import app.mortendahl.velib.library.contextaware.BaseContextAwareHandler;
import app.mortendahl.velib.library.contextaware.activity.ActivityUpdateEvent;
import app.mortendahl.velib.library.contextaware.geofence.GeofenceTransitionEvent;
import app.mortendahl.velib.service.guiding.GuidingService;
import app.mortendahl.velib.ui.MainActivity;

public class VelibContextAwareHandler extends BaseContextAwareHandler {

    @Override
    public void onActivityUpdate(ActivityUpdateEvent event) {

        Context context = VelibApplication.getCachedAppContext();

        if (event.onBicycle && event.confidence > 50) {
            // let the GuidingService handle the event since it knows best what it's currently doing
            GuidingService.bikingActivityAction.invoke(context);
        }

    }

    @Override
    public void onGeofenceTransition(GeofenceTransitionEvent event) {

        Context context = VelibApplication.getCachedAppContext();

        String fenceId = event.fenceIds.size() > 0 ? event.fenceIds.get(0) : "--";
        Notification transitionNotification = buildGeofenceTransitionNotification(context, fenceId, GeofenceTransitionEvent.describeTransition(event.transition));
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(303, transitionNotification);

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

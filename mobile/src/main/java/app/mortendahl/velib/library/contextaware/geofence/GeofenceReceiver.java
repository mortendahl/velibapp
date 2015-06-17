package app.mortendahl.velib.library.contextaware.geofence;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

import app.mortendahl.velib.R;
import app.mortendahl.velib.library.background.ActionHandler;
import app.mortendahl.velib.library.background.BaseBroadcastReceiver;
import app.mortendahl.velib.library.eventbus.EventStore;
import app.mortendahl.velib.network.jcdecaux.VelibStation;
import app.mortendahl.velib.ui.MainActivity;
import de.greenrobot.event.EventBus;

public class GeofenceReceiver extends BaseBroadcastReceiver {

    public GeofenceReceiver() {
        setActionHandlers(
                new BootActionHandler(),
                new GeofenceTransitionHandler()
        );
    }

    private static class BootActionHandler extends ActionHandler {

        @Override
        public String getAction() {
            return Intent.ACTION_BOOT_COMPLETED;
        }

        @Override
        public void handle(Context context, Intent intent) {
            GeofenceManager.refreshFencesAction.invoke(context);
            //
            // TODO should take a wake lock -- wakefulbroadcastreceiver?
            //
        }

    }

    public static class GeofenceTransitionHandler extends ActionHandler {

        private static final String ACTION = "geofence_transition";

        public static PendingIntent getPendingIntent(Context context) {
            Intent intent = new Intent(ACTION, null, context, GeofenceReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            return pendingIntent;
        }

        @Override
        public String getAction() {
            return ACTION;
        }

        @Override
        public void handle(Context context, Intent intent) {

            GeofencingEvent systemEvent = GeofencingEvent.fromIntent(intent);
            int fenceTransition = systemEvent.getGeofenceTransition();
            List<Geofence> triggeringGeofences = systemEvent.getTriggeringGeofences();

            ArrayList<String> fenceIds = new ArrayList<>();
            for (Geofence geofence : triggeringGeofences) {
                fenceIds.add(geofence.getRequestId());
            }

            GeofenceTransitionEvent event = new GeofenceTransitionEvent();
            event.transition = fenceTransition;
            event.fenceIds = fenceIds;

            EventStore.storeEvent(event);
            EventBus.getDefault().post(event);

            String fenceId = fenceIds.size() > 0 ? fenceIds.get(0) : "--";
            Notification transitionNotification = buildTransitionNotification(context, fenceId, GeofenceTransitionEvent.describeTransition(fenceTransition));
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(303, transitionNotification);

        }

        protected Notification buildTransitionNotification(Context context, String fenceId, String transition) {

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

}

package app.mortendahl.velib.library.contextaware.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

import app.mortendahl.velib.library.background.BaseBroadcastReceiver;
import app.mortendahl.velib.library.background.BroadcastReceiverActionHandler;
import app.mortendahl.velib.library.contextaware.ContextAwareApplication;
import de.greenrobot.event.EventBus;

public class GeofenceReceiver extends BaseBroadcastReceiver {

    public GeofenceReceiver() {
        setActionHandlers(
                new BootActionHandler(),
                new GeofenceTransitionHandler()
        );
    }

    private static class BootActionHandler extends BroadcastReceiverActionHandler {

        @Override
        public String getAction() {
            return Intent.ACTION_BOOT_COMPLETED;
        }

        @Override
        public void handle(Context context, Intent intent) {
            GeofenceManager.refreshFencesAction.invoke(context);
        }

    }

    public static class GeofenceTransitionHandler extends BroadcastReceiverActionHandler {

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
            // it can happen that the returned list is null
            if (triggeringGeofences != null) {
                for (Geofence geofence : triggeringGeofences) {
                    fenceIds.add(geofence.getRequestId());
                }
            }

            GeofenceTransitionEvent event = new GeofenceTransitionEvent();
            event.transition = fenceTransition;
            event.fenceIds = fenceIds;

            EventBus.getDefault().post(event);

            ContextAwareApplication app = (ContextAwareApplication) context.getApplicationContext();
            app.getContextAwareHandler().onGeofenceTransition(event);

        }

    }

}

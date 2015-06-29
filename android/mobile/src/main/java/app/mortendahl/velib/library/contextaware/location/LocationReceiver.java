package app.mortendahl.velib.library.contextaware.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderApi;

import app.mortendahl.velib.library.background.BaseBroadcastReceiver;
import app.mortendahl.velib.library.background.BroadcastReceiverActionHandler;
import app.mortendahl.velib.library.contextaware.ContextAwareApplication;
import de.greenrobot.event.EventBus;

public class LocationReceiver extends BaseBroadcastReceiver {

    public LocationReceiver() {
        setActionHandlers(
                new BootActionHandler(),
                new LocationUpdateHandler()
        );
    }

    private static class BootActionHandler extends BroadcastReceiverActionHandler {

        @Override
        public String getAction() {
            return Intent.ACTION_BOOT_COMPLETED;
        }

        @Override
        public void handle(Context context, Intent intent) {
            // don't do anything
            //LocationManager.frequencyAction.setInterval(context, -1);
        }

    }

    public static class LocationUpdateHandler extends BroadcastReceiverActionHandler {

        private static final String ACTION = "location_update";

        public static PendingIntent getPendingIntent(Context context) {
            Intent intent = new Intent(ACTION, null, context, LocationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            return pendingIntent;
        }

        @Override
        public String getAction() {
            return ACTION;
        }

        @Override
        public void handle(Context context, Intent intent) {

            Location location = (Location) intent.getExtras().get(FusedLocationProviderApi.KEY_LOCATION_CHANGED);
            if (location == null) { return; }

            LocationUpdateEvent event = new LocationUpdateEvent(location);

            EventBus.getDefault().post(event);

            ContextAwareApplication app = (ContextAwareApplication) context.getApplicationContext();
            app.getContextAwareHandler().onLocationUpdate(event);

        }

    }

}

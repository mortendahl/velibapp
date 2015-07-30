package app.mortendahl.velib.library.contextaware.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

import app.mortendahl.velib.library.background.BaseBroadcastReceiver;
import app.mortendahl.velib.library.background.BroadcastReceiverActionHandler;
import app.mortendahl.velib.library.contextaware.ContextAwareApplication;
import de.greenrobot.event.EventBus;

public class ActivityReceiver extends BaseBroadcastReceiver {

    public ActivityReceiver() {
        setActionHandlers(
                new BootActionHandler(),
                new ActivityUpdateHandler()
        );
    }

    private static class BootActionHandler extends BroadcastReceiverActionHandler {

        @Override
        public String getAction() {
            return Intent.ACTION_BOOT_COMPLETED;
        }

        @Override
        public void handle(Context context, Intent intent) {
            ActivityManager.frequencyAction.turnOn(context);
        }

    }

    public static class ActivityUpdateHandler extends BroadcastReceiverActionHandler {

        private static final String ACTION = "activity_update";

        public static PendingIntent getPendingIntent(Context context) {
            Intent intent = new Intent(ACTION, null, context, ActivityReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            return pendingIntent;
        }

        @Override
        public String getAction() {
            return ACTION;
        }

        @Override
        public void handle(Context context, Intent intent) {

            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            if (result == null) { return; }

            List<DetectedActivity> activities = result.getProbableActivities();
            ActivityUpdateEvent event = ActivityUpdateEvent.fromPlayActivities(activities);

            ContextAwareApplication app = (ContextAwareApplication) context.getApplicationContext();
            app.getContextAwareHandler().onActivityUpdate(event);

        }

    }

}

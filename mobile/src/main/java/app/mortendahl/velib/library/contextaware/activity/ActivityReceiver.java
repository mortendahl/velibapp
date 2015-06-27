package app.mortendahl.velib.library.contextaware.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import app.mortendahl.velib.library.background.ActionHandler;
import app.mortendahl.velib.library.background.BaseBroadcastReceiver;
import app.mortendahl.velib.library.contextaware.ContextAwareApplication;
import de.greenrobot.event.EventBus;

public class ActivityReceiver extends BaseBroadcastReceiver {

    public ActivityReceiver() {
        setActionHandlers(
                new BootActionHandler(),
                new ActivityUpdateHandler()
        );
    }

    private static class BootActionHandler extends ActionHandler {

        @Override
        public String getAction() {
            return Intent.ACTION_BOOT_COMPLETED;
        }

        @Override
        public void handle(Context context, Intent intent) {
            ActivityManager.frequencyAction.setInterval(context, 60);
        }

    }

    public static class ActivityUpdateHandler extends ActionHandler {

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

            DetectedActivity detectedActivity = result.getMostProbableActivity();

            ActivityUpdateEvent event = ActivityUpdateEvent.fromPlayActivity(detectedActivity);

            EventBus.getDefault().post(event);  // TODO move this to context aware handler?

            ContextAwareApplication app = (ContextAwareApplication) context.getApplicationContext();
            app.getContextAwareHandler().onActivityUpdate(event);

        }

    }

}

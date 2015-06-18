package app.mortendahl.velib.library.contextaware.activity;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import app.mortendahl.velib.R;
import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.library.background.ActionHandler;
import app.mortendahl.velib.library.background.BaseBroadcastReceiver;
import app.mortendahl.velib.library.eventbus.EventStore;
import app.mortendahl.velib.network.jcdecaux.Position;
import app.mortendahl.velib.service.GuidingService;
import app.mortendahl.velib.ui.MainActivity;
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
            // don't do anything
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

        private DetectedActivity previousBikingActivity = null;

        @Override
        public void handle(Context context, Intent intent) {

            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            if (result == null) { return; }

            DetectedActivity detectedActivity = result.getMostProbableActivity();
            int type = detectedActivity.getType();
            int confidence = detectedActivity.getConfidence();
            if (type == DetectedActivity.ON_BICYCLE
                && confidence > (previousBikingActivity != null ? previousBikingActivity.getConfidence() : 0)) {

                previousBikingActivity = detectedActivity;

                Notification bikingNotification = buildBikingNotification(context, confidence);
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(404, bikingNotification);

            } else {

                previousBikingActivity = null;

            }

            ActivityEvent event = ActivityEvent.fromPlayActivity(detectedActivity);
            EventStore.storeEvent(event);
            EventBus.getDefault().post(event);

        }

        private Notification buildBikingNotification(Context context, int confidence) {

            PendingIntent mainPendingIntent = MainActivity.getPendingIntent(context);

            Position workPosition = VelibApplication.POSITION_WORK;
            PendingIntent workPendingIntent = GuidingService.setDestinationAction.getPendingIntent(context, workPosition);

            Position gymPosition = VelibApplication.POSITION_GYM;
            PendingIntent gymPendingIntent = GuidingService.setDestinationAction.getPendingIntent(context, gymPosition);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Biking (" + confidence + "%)")
                    .setContentText("Biking")
                    .setContentIntent(mainPendingIntent)
                    .addAction(android.R.drawable.ic_media_play, "Work", workPendingIntent)
                    .addAction(android.R.drawable.ic_media_play, "Gym", gymPendingIntent);

            return notificationBuilder.build();

        }

    }

}

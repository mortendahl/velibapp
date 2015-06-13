package com.mortendahl.velib.contextaware.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderApi;
import com.mortendahl.velib.library.ActionHandler;
import com.mortendahl.velib.library.BaseBroadcastReceiver;

import de.greenrobot.event.EventBus;

public class LocationReceiver extends BaseBroadcastReceiver {

    protected static final String ACTION_LOCATION_UPDATE = "location_update";

    protected static PendingIntent getLocationUpdatePendingIntent(Context context) {
        Intent intent = new Intent(LocationReceiver.ACTION_LOCATION_UPDATE, null, context, LocationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public LocationReceiver() {
        setActionHandlers(
                new BootActionHandler(),
                new LocationUpdateHandler()
        );
    }

    public static class BootActionHandler extends ActionHandler {

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

    public static class LocationUpdateHandler extends ActionHandler {

        @Override
        public String getAction() {
            return ACTION_LOCATION_UPDATE;
        }

        @Override
        public void handle(Context context, Intent intent) {
            Location location = (Location) intent.getExtras().get(FusedLocationProviderApi.KEY_LOCATION_CHANGED);
            EventBus.getDefault().post(new LocationUpdatedEvent(location));
        }

    }



}

package com.mortendahl.velib.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.maps.model.LatLng;
import com.mortendahl.velib.network.jcdecaux.Position;
import com.mortendahl.velib.Logger;
import com.mortendahl.velib.R;
import com.mortendahl.velib.VelibApplication;
import com.mortendahl.velib.network.jcdecaux.VelibStation;
import com.mortendahl.velib.ui.MainActivity;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import de.greenrobot.event.EventBus;

public class GuidingService extends Service {

    private EventBusListener eventBusListener = new EventBusListener();
    protected Updator updator;

    private final HashMap<String, IntentHandler> actionHandlers;

    public GuidingService() {
        actionHandlers = new HashMap<>();
        actionHandlers.put(ClearDestinationHandler.ACTION, new ClearDestinationHandler());
        actionHandlers.put(SetDestinationHandler.ACTION, new SetDestinationHandler());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updator = new Updator();
        EventBus.getDefault().register(eventBusListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        boolean keepRunning = false;

        String action = (intent != null ? intent.getAction() : null);
        if (action != null) {
            IntentHandler handler = actionHandlers.get(action);
            keepRunning = handler.handle(intent);
        }

        if (keepRunning) {
            if (!EventBus.getDefault().isRegistered(eventBusListener)) { EventBus.getDefault().register(eventBusListener); }
            Notification notification = buildForegroundNotification();
            startForeground(101, notification);
            return START_STICKY;

        } else {
            EventBus.getDefault().unregister(eventBusListener);
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;

        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {}

    private Notification buildForegroundNotification() {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent stopIntent = new Intent(this, GuidingService.class);
        stopIntent.setAction(ClearDestinationHandler.ACTION);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);

        Notification notification;

        VelibStation station = (destination != null ? getBestStationForDestination() : null);

        if (station != null) {

            notification = new NotificationCompat.Builder(this)
                    .setContentTitle("Guiding")
                    .setTicker("Guiding")
                    .setContentText("" + station.name + "\n" + station.availableStands)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                    .build();

        } else {

            notification = new NotificationCompat.Builder(this)
                    .setContentTitle("Guiding")
                    .setTicker("Guiding")
                    .setContentText("Could not find best destination station")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();

        }

        return notification;

    }


    protected VelibStation getBestStationForDestination() {

        if (destination == null) { return null; }

        return Collections.min(VelibApplication.stationsMap.values(), new Comparator<VelibStation>() {

            // TODO this is a very inefficient way of getting distance!

            private float distance(Position source, Position destination) {

                Location sourceLocation = new Location("fake");
                sourceLocation.setLatitude(source.latitude);
                sourceLocation.setLongitude(source.longitude);

                Location destinationLocation = new Location("fake");
                destinationLocation.setLatitude(destination.latitude);
                destinationLocation.setLongitude(destination.longitude);

                return sourceLocation.distanceTo(destinationLocation);

            }

            @Override
            public int compare(VelibStation lhs, VelibStation rhs) {
                float lhsDistance = lhs.availableStands > 0 ? distance(lhs.position, destination) : Float.POSITIVE_INFINITY;
                float rhsDistance = rhs.availableBikes > 0 ? distance(rhs.position, destination) : Float.POSITIVE_INFINITY;
                return Float.compare(lhsDistance, rhsDistance);
            }

        });

    }







    private class Updator implements Runnable {

        private final Handler handler;

        public Updator() {
            handler = new Handler();
        }

        public void start() {
            handler.postDelayed(this, 0);
        }

        public void stop() {
            handler.removeCallbacks(this);
        }

        @Override
        public void run() {
            VelibApplication.reloadStations();
            handler.postDelayed(this, 5000);
        }

    }


    private class EventBusListener {

        public void onEvent(VelibStationUpdatedEvent event) {
            Logger.debug(Logger.TAG_GUI, this, event.getClass().getSimpleName());

            if (destination != null) {
                Notification notification = buildForegroundNotification();
                startForeground(101, notification);
            }

        }

        public void onEvent(MonitoredVelibStationsChangedEvent event) {
            Logger.debug(Logger.TAG_GUI, this, event.getClass().getSimpleName());
        }

    }





    protected Position destination = null;

    private abstract class IntentHandler {
        public abstract boolean handle(Intent intent);
    }


    public static void setDestination(Position destination) {
        Context context = VelibApplication.getCachedAppContext();
        Intent intent = new Intent(context, GuidingService.class);
        intent.setAction(SetDestinationHandler.ACTION);
        intent.putExtra(SetDestinationHandler.KEY_LATITUDE, destination.latitude);
        intent.putExtra(SetDestinationHandler.KEY_LONGITUDE, destination.longitude);
        context.startService(intent);
    }

    private class SetDestinationHandler extends IntentHandler {

        public static final String ACTION = "set_dest";
        public static final String KEY_LATITUDE = "dest_latitude";
        public static final String KEY_LONGITUDE = "dest_longitude";

        @Override
        public boolean handle(Intent intent) {

            Bundle bundle = intent.getExtras();
            if (!bundle.containsKey(KEY_LATITUDE)) { return false; }
            if (!bundle.containsKey(KEY_LONGITUDE)) { return false; }

            double latitude = bundle.getDouble(KEY_LATITUDE);
            double longitude = bundle.getDouble(KEY_LONGITUDE);
            destination = new Position(latitude, longitude);

            updator.start();

            return true;

        }
    }


    public static void clearDestination() {
        Context context = VelibApplication.getCachedAppContext();
        Intent intent = new Intent(context, GuidingService.class);
        intent.setAction(ClearDestinationHandler.ACTION);
        context.startService(intent);
    }

    private class ClearDestinationHandler extends IntentHandler {

        public static final String ACTION = "clear_dest";

        @Override
        public boolean handle(Intent intent) {

            destination = null;
            updator.stop();

            return false;
        }
    }

}

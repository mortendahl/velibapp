package com.mortendahl.velib.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.mortendahl.velib.Logger;
import com.mortendahl.velib.R;
import com.mortendahl.velib.VelibApplication;
import com.mortendahl.velib.network.jcdecaux.VelibStation;
import com.mortendahl.velib.ui.MainActivity;

import java.util.HashMap;

import de.greenrobot.event.EventBus;

public class GuidingService extends Service {

    private EventBusListener eventBusListener = new EventBusListener();
    protected Updator updator;

    private final HashMap<String, ActionHandler> actionHandlers;

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
            ActionHandler handler = actionHandlers.get(action);
            keepRunning = handler.handle(intent);
        }

        if (keepRunning) {
            Notification notification = buildForegroundNotification();
            startForeground(101, notification);
            EventBus.getDefault().register(eventBusListener);
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

        if (destination != null) {

            VelibStation station = VelibApplication.stationsMap.get(destination);

            notification = new NotificationCompat.Builder(this)
                    .setContentTitle("Guiding")
                    .setTicker("Guiding")
                    .setContentText("Destination " + station.name)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                    .build();

        } else {

            notification = new NotificationCompat.Builder(this)
                    .setContentTitle("Guiding")
                    .setTicker("Guiding")
                    .setContentText("No destination set")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();

        }

        return notification;

    }



    public static void setDestination(VelibStation station) {
        Context context = VelibApplication.getCachedAppContext();
        Intent intent = new Intent(context, GuidingService.class);
        intent.setAction(SetDestinationHandler.ACTION);
        intent.putExtra(SetDestinationHandler.KEY_STATIONNUMBER, station.number);
        context.startService(intent);
    }

    public static void clearDestination() {
        Context context = VelibApplication.getCachedAppContext();
        Intent intent = new Intent(context, GuidingService.class);
        intent.setAction(ClearDestinationHandler.ACTION);
        context.startService(intent);
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





    protected Integer destination = null;

    private abstract class ActionHandler {
        public abstract boolean handle(Intent intent);
    }

    private class SetDestinationHandler extends ActionHandler {

        public static final String ACTION = "set_dest";
        public static final String KEY_STATIONNUMBER = "station_number";

        @Override
        public boolean handle(Intent intent) {

            Bundle bundle = intent.getExtras();
            if (!bundle.containsKey(KEY_STATIONNUMBER)) { return false; }

            int stationNumber = bundle.getInt(KEY_STATIONNUMBER);
            destination = stationNumber;
            updator.start();

            return true;
        }
    }

    private class ClearDestinationHandler extends ActionHandler {

        public static final String ACTION = "clear_dest";

        @Override
        public boolean handle(Intent intent) {

            destination = null;
            updator.stop();

            return false;
        }
    }

}

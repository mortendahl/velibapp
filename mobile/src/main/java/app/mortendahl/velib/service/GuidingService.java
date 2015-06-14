package app.mortendahl.velib.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;

import app.mortendahl.velib.library.background.ActionHandler;
import app.mortendahl.velib.library.background.BaseService;
import app.mortendahl.velib.network.jcdecaux.Position;
import app.mortendahl.velib.Logger;
import app.mortendahl.velib.R;
import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.network.jcdecaux.VelibStation;
import app.mortendahl.velib.ui.MainActivity;

import java.util.Collections;
import java.util.Comparator;

import de.greenrobot.event.EventBus;

public class GuidingService extends BaseService {

    private EventBusListener eventBusListener = new EventBusListener();
//    protected Updator updator;

    public GuidingService() {
        setActionHandlers(
                new ClearDestinationHandler(this),
                new SetDestinationHandler(this)
        );
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        updator = new Updator();
    }

    @Override
    protected void onKeepRunningChanged(boolean keepRunning) {
        if (keepRunning) {
            if (!EventBus.getDefault().isRegistered(eventBusListener)) { EventBus.getDefault().register(eventBusListener); }

        } else {
            EventBus.getDefault().unregister(eventBusListener);

        }
    }

    protected Notification buildForegroundNotification() {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        PendingIntent stopPendingIntent = clearDestinationAction.getPendingIntent(this);

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







    protected class Updator implements Runnable {

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


    public static final SetDestinationHandler.Invoker setDestinationAction = new SetDestinationHandler.Invoker();

    public static class SetDestinationHandler extends ActionHandler {

        public static final String ACTION = "set_dest";
        public static final String KEY_LATITUDE = "dest_latitude";
        public static final String KEY_LONGITUDE = "dest_longitude";

        @Override
        public String getAction() {
            return ACTION;
        }

        public static class Invoker {
            public void invoke(Context context, Position destination) {
                Intent intent = new Intent(context, GuidingService.class);
                intent.setAction(SetDestinationHandler.ACTION);
                intent.putExtra(SetDestinationHandler.KEY_LATITUDE, destination.latitude);
                intent.putExtra(SetDestinationHandler.KEY_LONGITUDE, destination.longitude);
                context.startService(intent);
            }
        }

        protected final GuidingService state;

        public SetDestinationHandler(GuidingService state) {
            this.state = state;
        }

        @Override
        public Boolean handleSticky(Context context, Intent intent) {

            Bundle bundle = intent.getExtras();
            if (!bundle.containsKey(KEY_LATITUDE)) { return null; }
            if (!bundle.containsKey(KEY_LONGITUDE)) { return null; }

            double latitude = bundle.getDouble(KEY_LATITUDE);
            double longitude = bundle.getDouble(KEY_LONGITUDE);
            state.destination = new Position(latitude, longitude);

            StationUpdatorService.updatesAction.request(context);

            Notification notification = state.buildForegroundNotification();
            state.startForeground(101, notification);

            return true;

        }
    }



    public static final ClearDestinationHandler.Invoker clearDestinationAction = new ClearDestinationHandler.Invoker();

    public static class ClearDestinationHandler extends ActionHandler {

        public static final String ACTION = "clear_dest";

        @Override
        public String getAction() {
            return ACTION;
        }

        public static class Invoker {

            public void invoke(Context context) {
                Intent intent = new Intent(context, GuidingService.class);
                intent.setAction(ACTION);
                context.startService(intent);
            }

            public PendingIntent getPendingIntent(Context context) {
                Intent intent = new Intent(context, GuidingService.class);
                intent.setAction(ACTION);
                return PendingIntent.getService(context, 0, intent, 0);
            }

        }

        protected final GuidingService state;

        public ClearDestinationHandler(GuidingService state) {
            this.state = state;
        }

        @Override
        public Boolean handleSticky(Context context, Intent intent) {

            state.destination = null;
            StationUpdatorService.updatesAction.remove(context);

            return false;
        }
    }

}

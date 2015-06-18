package app.mortendahl.velib.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import app.mortendahl.velib.library.background.ActionHandler;
import app.mortendahl.velib.library.background.BaseService;
import app.mortendahl.velib.library.eventbus.EventStore;
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

    public GuidingService() {
        setActionHandlers(
                new ClearDestinationHandler(this),
                new SetDestinationHandler(this)
        );
    }

    @Override
    protected void onEnteringSticky() {
        Logger.debug(Logger.TAG_SERVICE, this, "onEnteringSticky");
        if (!EventBus.getDefault().isRegistered(eventBusListener)) { EventBus.getDefault().register(eventBusListener); }
    }

    @Override
    protected void onLeavingSticky() {
        Logger.debug(Logger.TAG_SERVICE, this, "onLeavingSticky");
        EventBus.getDefault().unregister(eventBusListener);
    }

    protected Notification buildForegroundNotification(VelibStation station) {

        PendingIntent mainPendingIntent = MainActivity.getPendingIntent(this);
        PendingIntent stopPendingIntent = GuidingService.clearDestinationAction.getPendingIntent(this);

        Notification notification;

        if (station != null) {

            notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Guiding")
                    .setTicker("Guiding")
                    .setContentText("" + station.name + "\n" + station.availableStands)
                    .setContentIntent(mainPendingIntent)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                    .build();

        } else {

            notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Guiding")
                    .setTicker("Guiding")
                    .setContentText("Could not find best destination station")
                    .setContentIntent(mainPendingIntent)
                    .setOngoing(true)
                    .build();

        }

        return notification;

    }

    protected Notification buildNewBestStationNotification(VelibStation bestStation) {

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        PendingIntent mapPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("New best station")
                .setContentText(bestStation.name)
                .setContentIntent(mapPendingIntent)
                .addAction(android.R.drawable.ic_menu_compass, "Map", mapPendingIntent);

        return notificationBuilder.build();

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





    /*** NOTE ***
     * the event bus listener is only receiving events while the service is started;
     * once stopped no events will be delivered anymore (since the state of a stopped
     * service is not consistent). As a result, any event that should start the service
     * must be sent through the intent pipeline, and NOT through the event bus.
     */
    private class EventBusListener {

        private Integer previousBestStation = null;

        public void onEvent(VelibStationUpdatedEvent event) {
            Logger.debug(Logger.TAG_GUI, this, event.getClass().getSimpleName());
            if (destination == null) { return; }

            VelibStation bestStation = getBestStationForDestination();

            Notification foregroundNotification = buildForegroundNotification(bestStation);
            startForeground(101, foregroundNotification);

            if (previousBestStation == null || previousBestStation != bestStation.number) {
                Notification newBestStationNotification = buildNewBestStationNotification(bestStation);
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                notificationManager.notify(202, newBestStationNotification);
            }
            previousBestStation = bestStation.number;

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
                Intent intent = getIntent(context, destination);
                context.startService(intent);
            }

            public PendingIntent getPendingIntent(Context context, Position destination) {
                Intent intent = getIntent(context, destination);
                return PendingIntent.getService(context, 0, intent, 0);
            }

            private Intent getIntent(Context context, Position destination) {
                Intent intent = new Intent(context, GuidingService.class);
                intent.setAction(SetDestinationHandler.ACTION);
                intent.putExtra(SetDestinationHandler.KEY_LATITUDE, destination.latitude);
                intent.putExtra(SetDestinationHandler.KEY_LONGITUDE, destination.longitude);
                return intent;
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
            Position destination = new Position(latitude, longitude);
            state.destination = destination;

            StationUpdatorService.updatesAction.request(context, GuidingService.class.getSimpleName());

            VelibStation station = state.getBestStationForDestination();
            Notification notification = state.buildForegroundNotification(station);
            state.startForeground(101, notification);

            SetDestinationEvent event = new SetDestinationEvent(destination);
            EventStore.storeEvent(event);
            EventBus.getDefault().post(event);

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
            StationUpdatorService.updatesAction.remove(context, GuidingService.class.getSimpleName());

            ClearDestinationEvent event = new ClearDestinationEvent();
            EventStore.storeEvent(event);
            EventBus.getDefault().post(event);

            return false;
        }
    }

}

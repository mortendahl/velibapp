package app.mortendahl.velib.service.guiding;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import app.mortendahl.velib.VelibContextAwareHandler;
import app.mortendahl.velib.library.background.BaseService;
import app.mortendahl.velib.library.background.ServiceActionHandler;
import app.mortendahl.velib.network.jcdecaux.Position;
import app.mortendahl.velib.Logger;
import app.mortendahl.velib.R;
import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.network.jcdecaux.VelibStation;
import app.mortendahl.velib.service.MonitoredVelibStationsChangedEvent;
import app.mortendahl.velib.service.data.DataStore;
import app.mortendahl.velib.service.stationupdator.StationUpdatorService;
import app.mortendahl.velib.service.stationupdator.VelibStationUpdatedEvent;
import app.mortendahl.velib.ui.MainActivity;
import de.greenrobot.event.EventBus;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class GuidingService extends BaseService {

    private EventBusListener eventBusListener = new EventBusListener();

    public GuidingService() {
        setActionHandlers(
                new ClearDestinationAction.Handler(this),
                new SetDestinationAction.Handler(this),
                new BikingActivityAction.Handler(this)
        );
    }

    protected GoogleApiClient googleApiClient;
    private GoogleApiClientCallbacks googleApiClientCallbacks = new GoogleApiClientCallbacks();

    @Override
    public void onCreate() {
        super.onCreate();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(Wearable.API)
                .addConnectionCallbacks(googleApiClientCallbacks)
                .addOnConnectionFailedListener(googleApiClientCallbacks)
                .build();
    }

    @Override
    protected void onEnteringSticky() {
        Logger.debug(Logger.TAG_SERVICE, this, "onEnteringSticky");
        if (!EventBus.getDefault().isRegistered(eventBusListener)) { EventBus.getDefault().register(eventBusListener); }
        if (!googleApiClient.isConnected()) { googleApiClient.connect(); }
    }

    @Override
    protected void onLeavingSticky() {
        Logger.debug(Logger.TAG_SERVICE, this, "onLeavingSticky");
        EventBus.getDefault().unregister(eventBusListener);
        googleApiClient.disconnect();
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

        Collection<VelibStation> stations = VelibApplication.getSessionStore().stationsMap.values();
        if (stations.size() < 1) { return null; }

        return Collections.min(stations, new Comparator<VelibStation>() {

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

    protected void updateWearDataApi(VelibStation bestStation) {

        final String BEST_DESTINATION_PATH = "/best_dest";
        final String BEST_DESTINATION_NAME = "best_dest_name";
        final String BEST_DESTINATION_LATITUDE = "best_dest_latitude";
        final String BEST_DESTINATION_LONGITUDE = "best_dest_longitude";

        if (googleApiClient.isConnected()) {

            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(BEST_DESTINATION_PATH);
            putDataMapRequest.getDataMap().putString(BEST_DESTINATION_NAME, bestStation.name);
            putDataMapRequest.getDataMap().putDouble(BEST_DESTINATION_LATITUDE, bestStation.position.latitude);
            putDataMapRequest.getDataMap().putDouble(BEST_DESTINATION_LONGITUDE, bestStation.position.longitude);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();

            Wearable.DataApi.putDataItem(googleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult result) {
                            Logger.debug(Logger.TAG_SERVICE, GuidingService.class, "putDataItem, " + result.getStatus().getStatusMessage());
                        }
                    });

        }

    }



    private class GoogleApiClientCallbacks implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnected(Bundle connectionHint) {

        }

        @Override
        public void onConnectionSuspended(int cause) {

        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }

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

            // ignore if no destination is currently set
            if (destination == null) { return; }

            // ignore if there is no best station (maybe we don't have station data)
            VelibStation bestStation = getBestStationForDestination();
            if (bestStation == null) { return; }

            // create/update foreground notification
            Notification foregroundNotification = buildForegroundNotification(bestStation);
            startForeground(101, foregroundNotification);

            // notify if best station has changed
            if (previousBestStation == null || previousBestStation != bestStation.number) {
                Notification newBestStationNotification = buildNewBestStationNotification(bestStation);
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                notificationManager.notify(202, newBestStationNotification);
            }
            previousBestStation = bestStation.number;

            // update info on any wearable devices that may be listening (now or in the future)
            updateWearDataApi(bestStation);

        }

        public void onEvent(MonitoredVelibStationsChangedEvent event) {
            Logger.debug(Logger.TAG_GUI, this, event.getClass().getSimpleName());
        }

    }







    protected Position destination = null;


    public static final SetDestinationAction.Invoker setDestinationAction = new SetDestinationAction.Invoker();

    public static class SetDestinationAction {

        public static final String ACTION = "set_dest";
        public static final String KEY_LATITUDE = "dest_latitude";
        public static final String KEY_LONGITUDE = "dest_longitude";

        public static class Invoker {

            public void invoke(Context context, double latitude, double longitude) {
                Intent intent = getIntent(context, latitude, longitude);
                context.startService(intent);
            }

            public PendingIntent getPendingIntent(Context context, Position destination) {
                Intent intent = getIntent(context, destination.latitude, destination.longitude);
                return PendingIntent.getService(context, 0, intent, 0);
            }

            private Intent getIntent(Context context, double latitude, double longitude) {
                Intent intent = new Intent(context, GuidingService.class);
                intent.setAction(SetDestinationAction.ACTION);
                intent.putExtra(SetDestinationAction.KEY_LATITUDE, latitude);
                intent.putExtra(SetDestinationAction.KEY_LONGITUDE, longitude);
                return intent;
            }

        }

        public static class Handler extends ServiceActionHandler {

            private final GuidingService state;

            public Handler(GuidingService state) {
                this.state = state;
            }

            @Override
            public String getAction() {
                return ACTION;
            }

            @Override
            public Boolean handle(Context context, Intent intent) {

                Bundle bundle = intent.getExtras();
                if (!bundle.containsKey(KEY_LATITUDE)) { return null; }
                if (!bundle.containsKey(KEY_LONGITUDE)) { return null; }

                double latitude = bundle.getDouble(KEY_LATITUDE);
                double longitude = bundle.getDouble(KEY_LONGITUDE);
                Position destination = new Position(latitude, longitude);
                state.destination = destination;

                StationUpdatorService.updatesAction.request(context, GuidingService.class.getSimpleName());

                VelibStation station = state.getBestStationForDestination();
                if (station == null) { return null; }

                Notification notification = state.buildForegroundNotification(station);
                state.startForeground(101, notification);

                SetDestinationEvent event = new SetDestinationEvent(destination);
                DataStore.getCollection(VelibContextAwareHandler.eventStoreId).append(event);
                EventBus.getDefault().post(event);

                return true;

            }

        }

    }



    public static final ClearDestinationAction.Invoker clearDestinationAction = new ClearDestinationAction.Invoker();

    public static class ClearDestinationAction {

        public static final String ACTION = "clear_dest";

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

        public static class Handler extends ServiceActionHandler {

            private final GuidingService state;

            public Handler(GuidingService state) {
                this.state = state;
            }

            @Override
            public String getAction() {
                return ACTION;
            }

            @Override
            public Boolean handle(Context context, Intent intent) {

                state.destination = null;
                StationUpdatorService.updatesAction.remove(context, GuidingService.class.getSimpleName());

                ClearDestinationEvent event = new ClearDestinationEvent();
                DataStore.getCollection(VelibContextAwareHandler.eventStoreId).append(event);
                EventBus.getDefault().post(event);

                return false;
            }

        }

    }



    public static final BikingActivityAction.Invoker bikingActivityAction = new BikingActivityAction.Invoker();

    public static class BikingActivityAction {

        protected static final String ACTION = "biking_activity";

        public static class Invoker {

            public void invoke(Context context) {
                Intent intent = new Intent(context, GuidingService.class);
                intent.setAction(ACTION);
                context.startService(intent);
            }

        }

        public static class Handler extends ServiceActionHandler {

            private final GuidingService state;

            public Handler(GuidingService state) {
                this.state = state;
            }

            @Override
            public String getAction() {
                return ACTION;
            }

            @Override
            public Boolean handle(Context context, Intent intent) {

                if (state.destination != null) {

                    // already running so don't do anything differently
                    return null;

                } else {

                    // not running so ..

                    // .. show notification to start guiding
                    Notification bikingNotification = buildBikingNotification(context);
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    notificationManager.notify(404, bikingNotification);

                    // .. and go back to sleep
                    return false;

                }
            }

            private Notification buildBikingNotification(Context context) {

                PendingIntent mainPendingIntent = MainActivity.getPendingIntent(context);

                Position workPosition = VelibApplication.POSITION_WORK;
                PendingIntent workPendingIntent = GuidingService.setDestinationAction.getPendingIntent(context, workPosition);

                Position gymPosition = VelibApplication.POSITION_GYM;
                PendingIntent gymPendingIntent = GuidingService.setDestinationAction.getPendingIntent(context, gymPosition);

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Biking")
                        .setContentText("Biking")
                        .setContentIntent(mainPendingIntent)
                        .addAction(android.R.drawable.ic_media_play, "Work", workPendingIntent)
                        .addAction(android.R.drawable.ic_media_play, "Gym", gymPendingIntent);

                return notificationBuilder.build();

            }

        }

    }

}

package app.mortendahl.velib.service.guiding;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

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
import app.mortendahl.velib.library.contextaware.location.LocationManager;
import app.mortendahl.velib.network.jcdecaux.Position;
import app.mortendahl.velib.Logger;
import app.mortendahl.velib.R;
import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.network.jcdecaux.VelibStation;
import app.mortendahl.velib.service.data.DataStore;
import app.mortendahl.velib.service.data.SuggestedDestination;
import app.mortendahl.velib.service.stationupdator.StationUpdatorService;
import app.mortendahl.velib.service.stationupdator.VelibStationUpdatedEvent;
import app.mortendahl.velib.service.stationupdator.VelibStationsChangedEvent;
import app.mortendahl.velib.ui.main.MainActivity;
import de.greenrobot.event.EventBus;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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



    private Notification buildForegroundNotification(VelibStation station) {

        PendingIntent mainPendingIntent = MainActivity.getPendingIntent(this);
        PendingIntent stopPendingIntent = GuidingService.clearDestinationAction.getPendingIntent(this);

        Notification notification;

        if (station != null) {

            notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.notification_guiding_title))
                    .setTicker(getString(R.string.notification_guiding_title))
                    .setContentText(getString(R.string.notification_guiding_text_station, station.name, station.availableStands))
                    .setContentIntent(mainPendingIntent)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                    .build();

        } else {

            notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.notification_guiding_title))
                    .setTicker(getString(R.string.notification_guiding_title))
                    .setContentText(getString(R.string.notification_guiding_text_nostation))
                    .setContentIntent(mainPendingIntent)
                    .setOngoing(true)
                    .build();

        }

        return notification;

    }

    private Notification buildNewBestStationNotification(VelibStation bestStation) {

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

    private VelibStation getBestStationForDestination() {

        Collection<VelibStation> stations = VelibApplication.getDataStore().stationsMap.values();
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

    private void updateWearDataApi(VelibStation bestStation) {

        final String BEST_DESTINATION_PATH = "/best_dest";
        final String BEST_DESTINATION_NAME = "best_dest_name";
        final String BEST_DESTINATION_STANDS = "best_dest_stands";
        final String BEST_DESTINATION_LATITUDE = "best_dest_latitude";
        final String BEST_DESTINATION_LONGITUDE = "best_dest_longitude";

        if (!googleApiClient.isConnected()) { return; }

        if (bestStation != null) {

            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(BEST_DESTINATION_PATH);
            putDataMapRequest.getDataMap().putString(BEST_DESTINATION_NAME, bestStation.name);
            putDataMapRequest.getDataMap().putInt(BEST_DESTINATION_STANDS, bestStation.availableStands);
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

        } else {

//            Wearable.DataApi.getDataItems(googleApiClient)
//                    .setResultCallback(new ResultCallback<DataItemBuffer>() {
//                        @Override
//                        public void onResult(DataItemBuffer dataItems) {
//                            Logger.debug(Logger.TAG_SERVICE, GuidingService.class, "deleteDataItems, onResult, " + dataItems.getCount());
//                            for (DataItem dataItem : dataItems) {
//                                Uri uri = dataItem.getUri();
//                                String path = uri.getPath();
//                                Logger.debug(Logger.TAG_SERVICE, GuidingService.class, "deleteDataItems, onResult, " + path);
//                                if ("/best_dest".equals(path)) {
//                                    Wearable.DataApi.deleteDataItems(googleApiClient, uri)
//                                            .setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
//                                                @Override
//                                                public void onResult(DataApi.DeleteDataItemsResult result) {
//                                                    Logger.debug(Logger.TAG_SERVICE, GuidingService.class, "deleteDataItems, " + result.getStatus().getStatusMessage());
//                                                }
//                                            });
//
//                                }
//                            }
//                        }
//                    });


            Uri uri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path("/best_dest").build();
            Wearable.DataApi.deleteDataItems(googleApiClient, uri)
                    .setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
                        @Override
                        public void onResult(DataApi.DeleteDataItemsResult result) {
                            // never called (service dead before it happens?) but seems to work somewhat
                            Logger.debug(Logger.TAG_SERVICE, GuidingService.class, "deleteDataItems, " + result.getStatus().getStatusMessage());
                        }
                    });

        }

    }




    protected void setDestination(Position destination) {
        this.destination = destination;
        refresh();
    }

    protected boolean isGuiding() {
        return this.destination != null;
    }




    private Integer previousBestStation = null;

    protected void refresh() {

        VelibStation bestStation = null;
        if (destination != null) {

            bestStation = getBestStationForDestination();

            // create/update foreground notification
            Notification foregroundNotification = buildForegroundNotification(bestStation);
            startForeground(101, foregroundNotification);

            // notify if best station has changed
            if (bestStation != null) {
                if (previousBestStation == null || previousBestStation != bestStation.number) {
                    Notification newBestStationNotification = buildNewBestStationNotification(bestStation);
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                    notificationManager.notify(202, newBestStationNotification);
                }
                previousBestStation = bestStation.number;
            }

        }

        // update info on any wearable devices that may be listening (now or in the future)
        updateWearDataApi(bestStation);

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
     * once stopped no events will be delivered anymore (since the service of a stopped
     * service is not consistent). As a result, any event that should start the service
     * must be sent through the intent pipeline, and NOT through the event bus.
     */
    private class EventBusListener {

        public void onEvent(VelibStationUpdatedEvent event) {
            refresh();
        }

        public void onEvent(VelibStationsChangedEvent event) {
            refresh();
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
                return getPendingIntent(context, destination.latitude, destination.longitude);
            }

            public PendingIntent getPendingIntent(Context context, double latitude, double longitude) {
                Intent intent = getIntent(context, latitude, longitude);
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
                state.setDestination(destination);

                SetDestinationEvent event = new SetDestinationEvent(destination);
                DataStore.getCollection(VelibContextAwareHandler.eventStoreId).append(event);
                EventBus.getDefault().post(event);

                LocationManager.frequencyAction.turnHigh(context);
                StationUpdatorService.updatesAction.request(context, GuidingService.class.getSimpleName());

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

                state.setDestination(null);
                LocationManager.frequencyAction.turnOff(context);
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

            private final GuidingService service;

            public Handler(GuidingService service) {
                this.service = service;
            }

            @Override
            public String getAction() {
                return ACTION;
            }

            @Override
            public Boolean handle(Context context, Intent intent) {

                if (service.isGuiding()) {

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

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(context.getString(R.string.notification_biking_detected_title))
                        .setContentText(context.getString(R.string.notification_biking_detected_text))
                        .setContentIntent(mainPendingIntent);

                List<SuggestedDestination> predictedDestinations = VelibApplication.getDataStore().predictedDestinations.getAll();

                // add actions for handheld
                if (predictedDestinations.size() >= 1) {
                    SuggestedDestination mostLikelyDestination = predictedDestinations.get(0);
                    // extract info
                    double latitude = mostLikelyDestination.latitude;
                    double longitude = mostLikelyDestination.longitude;
                    String title = context.getString(R.string.notification_mostlikelydestination_title, mostLikelyDestination);
                    // build pending intent and add as action to notification
                    PendingIntent pendingIntent = GuidingService.setDestinationAction.getPendingIntent(context, latitude, longitude);
                    notificationBuilder.addAction(android.R.drawable.ic_media_play, title, pendingIntent);
                }

                // add actions for wearable (this will prevent handheld actions from being shown)
                NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();
                for (SuggestedDestination predictedDestination : predictedDestinations) {
                    // extract info
                    double latitude = predictedDestination.latitude;
                    double longitude = predictedDestination.longitude;
                    String title = predictedDestination.getPrimaryAddressLine();
                    // build pending intent and add as action to notification
                    PendingIntent pendingIntent = GuidingService.setDestinationAction.getPendingIntent(context, latitude, longitude);
                    wearableExtender.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play, title, pendingIntent));
                }
                notificationBuilder.extend(wearableExtender);

                return notificationBuilder.build();

            }

        }

    }

}

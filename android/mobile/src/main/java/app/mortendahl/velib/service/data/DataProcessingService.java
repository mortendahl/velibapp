package app.mortendahl.velib.service.data;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;

import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.Logger;
import app.mortendahl.velib.VelibContextAwareHandler;
import app.mortendahl.velib.library.background.BaseIntentService;
import app.mortendahl.velib.library.background.IntentServiceActionHandler;
import app.mortendahl.velib.network.jcdecaux.Position;
import app.mortendahl.velib.service.guiding.SetDestinationEvent;
import de.greenrobot.event.EventBus;

public class DataProcessingService extends BaseIntentService {

    public DataProcessingService() {
        setActionHandlers(
                new RefreshSuggestedDestinations.Handler(),
                new RefreshRecentDestinations.Handler()
        );
    }

    public static final RefreshSuggestedDestinations.Invoker refreshSuggestedDestinationsAction = new RefreshSuggestedDestinations.Invoker();
    public static final RefreshRecentDestinations.Invoker refreshRecentDestinationsAction = new RefreshRecentDestinations.Invoker();

    public static class RefreshSuggestedDestinations {

        protected static final String ACTION = "refresh_suggested_destinations";

        public static class Invoker {

            public void invoke(Context context) {
                Intent intent = new Intent(context, DataProcessingService.class);
                intent.setAction(RefreshSuggestedDestinations.ACTION);
                context.startService(intent);
            }

        }

        public static class Handler extends IntentServiceActionHandler {

            @Override
            public String getAction() {
                return ACTION;
            }

            @Override
            public void handle(Context context, Intent intent) {

                // load previous destinations
                ArrayList<PreviousDestination> previousDestinations = loadPreviousDestinations();

                // find suggestions based on these
                ArrayList<SuggestedDestination> suggestedDestinations = clusterPreviousDestinations(previousDestinations);

                // sort by weight (highest first)
                Collections.sort(suggestedDestinations, new Comparator<SuggestedDestination>() {
                    @Override
                    public int compare(SuggestedDestination lhs, SuggestedDestination rhs) {
                        return -1 * Double.compare(lhs.weight, rhs.weight);
                    }
                });

                // reverse geocode suggestions
                suggestedDestinations = reverseGeocode(context, suggestedDestinations);

                // store result
                storeSuggestedDestinations(suggestedDestinations);

                // broadcast update
                EventBus.getDefault().post(new SuggestedDestinationsUpdatedEvent());

            }

            private ArrayList<SuggestedDestination> clusterPreviousDestinations(ArrayList<PreviousDestination> previousDestinations) {

                NonHierarchicalDistanceBasedAlgorithm<PreviousDestination> clusterAlgo = new NonHierarchicalDistanceBasedAlgorithm();
                clusterAlgo.addItems(previousDestinations);
                Set<? extends Cluster<PreviousDestination>> clusters = clusterAlgo.getClusters(14d);

                ArrayList<SuggestedDestination> clusterCenters = new ArrayList();
                for (Cluster<PreviousDestination> cluster : clusters) {
                    double count = cluster.getItems().size();
                    double lats = 0d;
                    double lons = 0d;

                    for (PreviousDestination dest : cluster.getItems()) {
                        lats += dest.position.latitude;
                        lons += dest.position.longitude;
                    }

                    double latCenter = lats/count;
                    double lonCenter = lons/count;

                    SuggestedDestination suggestedDestination = new SuggestedDestination(latCenter, lonCenter);
                    suggestedDestination.weight = count;
                    clusterCenters.add(suggestedDestination);
                }

                return clusterCenters;

            }

            private void storeSuggestedDestinations(ArrayList<SuggestedDestination> suggestedDestinations) {
                VelibApplication.getDataStore().predictedDestinations.replace(suggestedDestinations);
            }

        }

    }

    public static class RefreshRecentDestinations {

        protected static final int NUMBER_OF_RECENT_DESTINATIONS = 15;
        protected static final String ACTION = "refresh_recent_destinations";

        public static class Invoker {

            public void invoke(Context context) {
                Intent intent = new Intent(context, DataProcessingService.class);
                intent.setAction(RefreshRecentDestinations.ACTION);
                context.startService(intent);
            }

        }

        public static class Handler extends IntentServiceActionHandler {

            @Override
            public String getAction() {
                return ACTION;
            }

            @Override
            public void handle(Context context, Intent intent) {

                // load previous destinations
                ArrayList<PreviousDestination> previousDestinations = loadPreviousDestinations();

                // sort by timestamp
                Collections.sort(previousDestinations, new Comparator<PreviousDestination>() {
                    @Override
                    public int compare(PreviousDestination lhs, PreviousDestination rhs) {
                        return -1 * Long.valueOf(lhs.timestamp).compareTo(rhs.timestamp);
                    }
                });

                // keep only #NUMBER_OF_RECENT_DESTINATIONS most recent
                if (previousDestinations.size() > NUMBER_OF_RECENT_DESTINATIONS) {
                    previousDestinations.subList(NUMBER_OF_RECENT_DESTINATIONS, previousDestinations.size()).clear();
                    previousDestinations.trimToSize();
                }

                // convert to suggested destinations
                ArrayList<SuggestedDestination> recentDestinations = new ArrayList<>(previousDestinations.size());
                for (PreviousDestination previousDestination : previousDestinations) {

                    double latitude = previousDestination.getPosition().latitude;
                    double longitude = previousDestination.getPosition().longitude;

                    SuggestedDestination recentDestination = new SuggestedDestination(latitude, longitude);
                    recentDestination.timestamp = previousDestination.timestamp;
                    recentDestinations.add(recentDestination);

                }

                // add addresses
                reverseGeocode(context, recentDestinations);

                // store result
                VelibApplication.getDataStore().recentDestinations.replace(recentDestinations);

                // broadcast update
                EventBus.getDefault().post(new RecentDestinationsUpdatedEvent());

            }

        }

    }

    protected static ArrayList<PreviousDestination> loadPreviousDestinations() {

        ArrayList<PreviousDestination> previousDestinations = new ArrayList<>();

        for (JSONObject jsonEvent : DataStore.getCollection(VelibContextAwareHandler.eventStoreId).loadAll()) {
            try {

                if ( ! SetDestinationEvent.class.getSimpleName().equals(jsonEvent.getString("class"))) { continue; }
                double latitude = jsonEvent.getDouble("latitude");
                double longitude = jsonEvent.getDouble("longitude");
                Position position = new Position(latitude, longitude);
                long timestamp = jsonEvent.getLong("timestamp");

                previousDestinations.add(new PreviousDestination(position, timestamp));

            } catch (JSONException e) {
                Logger.error(Logger.TAG_GUI, DataProcessingService.class, e);
            }
        }

        return previousDestinations;

    }

    protected static ArrayList<SuggestedDestination> reverseGeocode(Context context, ArrayList<SuggestedDestination> suggestedDestinations) {

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        for (SuggestedDestination destination : suggestedDestinations) {

            try {

                List<Address> addresses = geocoder.getFromLocation(destination.latitude, destination.longitude, 1);
                if (addresses != null && addresses.size() >= 1) {
                    destination.setAddress(addresses.get(0));
                }

            } catch (Exception e) {
                Logger.error(Logger.TAG_GUI, DataProcessingService.class, e);
            }

        }

        return suggestedDestinations;

    }

}

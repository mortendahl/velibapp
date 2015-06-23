package app.mortendahl.velib.ui.list;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;

import org.json.JSONException;
import org.json.JSONObject;

import app.mortendahl.velib.library.eventbus.EventSystem;
import app.mortendahl.velib.Logger;
import app.mortendahl.velib.R;
import app.mortendahl.velib.network.jcdecaux.Position;
import app.mortendahl.velib.service.guiding.GuidingService;
import app.mortendahl.velib.service.guiding.SetDestinationEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StationListFragment extends Fragment implements AbsListView.OnItemClickListener {

    public static StationListFragment newInstance() {
        StationListFragment fragment = new StationListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private AbsListView listView;

    protected ArrayList<SuggestedDestination> items = new ArrayList<>();
    protected ArrayAdapter<SuggestedDestination> adapter;

    public StationListFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {}

        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, items);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        geocoder = new Geocoder(activity.getApplicationContext(), Locale.getDefault());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item, container, false);

        listView = (AbsListView) view.findViewById(android.R.id.list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        View emptyView = listView.getEmptyView();
        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText("empty");
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadList();
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelReverseGeocodeTasks();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SuggestedDestination chosenDestination = items.get(position);
        Logger.debug(Logger.TAG_GUI, this, chosenDestination.getPrimaryAddressLine());
        GuidingService.setDestinationAction.invoke(getActivity(), chosenDestination.position);
    }

    private ArrayList<SuggestedDestination> getSuggestedDestinations() {

        //
        // load previous destinations
        //

        ArrayList<PreviousDestination> previousDestinations = new ArrayList<>();

        for (JSONObject jsonEvent : EventSystem.loadAll()) {
            try {

                if ( ! SetDestinationEvent.class.getSimpleName().equals(jsonEvent.getString("class"))) { continue; }
                double latitude = jsonEvent.getDouble("latitude");
                double longitude = jsonEvent.getDouble("longitude");
                Position position = new Position(latitude, longitude);

                previousDestinations.add(new PreviousDestination(position));

            } catch (JSONException e) {
                Logger.error(Logger.TAG_GUI, this, e);
            }
        }

        //
        // make suggestions based on these
        //

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

            clusterCenters.add(new SuggestedDestination(count, latCenter, lonCenter));
        }

        return clusterCenters;
    }

    private void reloadList() {

        // clean up existing
        cancelReverseGeocodeTasks();
        items.clear();

        ArrayList<SuggestedDestination> suggestedDestinations = getSuggestedDestinations();
        Collections.sort(suggestedDestinations, new Comparator<SuggestedDestination>() {
            @Override
            public int compare(SuggestedDestination lhs, SuggestedDestination rhs) {
                return -1 * Double.compare(lhs.weight, rhs.weight);
            }
        });

        for (SuggestedDestination destination : suggestedDestinations) {

            // add to list
            items.add(destination);

            // ... and create and launch reverse geocoding task
            ReverseGeocodeDestinationTask task = new ReverseGeocodeDestinationTask(destination);
            geocodingTasks.add(task);
            task.execute();

        }

        // notify adapter about changes
        adapter.notifyDataSetChanged();

    }

    private void cancelReverseGeocodeTasks() {
        for (ReverseGeocodeDestinationTask task : geocodingTasks) {
            task.cancel(true);
        }
        geocodingTasks.clear();
    }

    protected Geocoder geocoder;

    private ArrayList<ReverseGeocodeDestinationTask> geocodingTasks = new ArrayList<>();

    private class PreviousDestination implements ClusterItem {

        private final Position position;
        private LatLng cachedLatLng = null;

        public PreviousDestination(Position position) {
            this.position = position;
        }

        @Override
        public LatLng getPosition() {
            if (cachedLatLng == null) {
                cachedLatLng = new LatLng(position.latitude, position.longitude);
            }
            return cachedLatLng;
        }

    }

    private class SuggestedDestination {

        public final double weight;
        public final Position position;
        public Address address = null;

        public SuggestedDestination(double weight, double latitude, double longitude) {
            this.weight = weight;
            this.position = new Position(latitude, longitude);
        }

        public String getPrimaryAddressLine() {
            String lineOne = address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "";
            return lineOne;
        }

        public String getSecondaryAddressLine() {

            String postalCode = address.getPostalCode();
            String locality = address.getLocality();
            String countryName = address.getCountryName();
            String lineTwo = null;
            if (postalCode != null && !postalCode.isEmpty()) {
                lineTwo = postalCode;
            }
            if (locality != null && !locality.isEmpty()) {
                lineTwo = (lineTwo != null ? lineTwo + ", " + locality : locality);
            }
            if (countryName != null && !countryName.isEmpty()) {
                lineTwo = (lineTwo != null ? lineTwo + ", " + countryName : countryName);
            }

            return lineTwo;
        }

        @Override
        public String toString() {
            return address != null ? getPrimaryAddressLine() : String.format("%f, %f", position.latitude, position.longitude);
        }

    }

    private class ReverseGeocodeDestinationTask extends AsyncTask<Void, Void, Address> {

        private final SuggestedDestination item;

        public ReverseGeocodeDestinationTask(SuggestedDestination item) {
            this.item = item;
        }

        @Override
        protected Address doInBackground(Void... params) {

            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(item.position.latitude, item.position.longitude, 1);
            } catch (Exception e) {
                Logger.error(Logger.TAG_GUI, this, e);
                return null;
            }

            if (addresses == null || addresses.size() < 1) { return null; }

            // return the first address
            return addresses.get(0);
        }

        // NOTE runs on UI thread, so safe to update data model
        @Override
        protected void onPostExecute(Address address) {
            if (address == null) { return; }
            item.address = address;
            adapter.notifyDataSetChanged();
        }

    }

    private class SuggestedDestinationsAdapter extends ArrayAdapter<SuggestedDestination> {

        public SuggestedDestinationsAdapter(Context context, int resource, int textViewResourceId, List<SuggestedDestination> items) {
            super(context, resource, textViewResourceId, items);
        }
    }

}

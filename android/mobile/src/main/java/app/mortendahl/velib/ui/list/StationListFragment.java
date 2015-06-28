package app.mortendahl.velib.ui.list;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import app.mortendahl.velib.Logger;
import app.mortendahl.velib.R;
import app.mortendahl.velib.service.data.DataProcessingService;
import app.mortendahl.velib.service.data.DataStore;
import app.mortendahl.velib.service.data.SuggestedDestinationsUpdatedEvent;
import app.mortendahl.velib.service.guiding.GuidingService;
import app.mortendahl.velib.service.data.SuggestedDestination;
import de.greenrobot.event.EventBus;

import java.util.ArrayList;
import java.util.List;

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
        EventBus.getDefault().register(eventBusListener);
    }

    @Override
    public void onPause() {
        super.onResume();
        EventBus.getDefault().unregister(eventBusListener);
    }

    private EventBusListener eventBusListener = new EventBusListener();

    private class EventBusListener {

        public void onEventMainThread(SuggestedDestinationsUpdatedEvent event) {
            reloadList();
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SuggestedDestination chosenDestination = items.get(position);
        Logger.debug(Logger.TAG_GUI, this, chosenDestination.getPrimaryAddressLine());
        GuidingService.setDestinationAction.invoke(getActivity(), chosenDestination.latitude, chosenDestination.longitude);
    }

    protected void reloadList() {

        // clean up existing
        items.clear();

        List<JSONObject> rawSuggestedDestinations = DataStore.getCollection(DataProcessingService.STOREID_SUGGESTED_DESTINATIONS).loadAll();
        for (JSONObject rawDestination : rawSuggestedDestinations) {
            try {

                SuggestedDestination destination = SuggestedDestination.fromJson(rawDestination);
                items.add(destination);

            } catch (JSONException e) {
                Logger.error(Logger.TAG_GUI, this, e);
            }
        }

        // notify adapter about changes
        adapter.notifyDataSetChanged();

    }

    private class SuggestedDestinationsAdapter extends ArrayAdapter<SuggestedDestination> {

        public SuggestedDestinationsAdapter(Context context, int resource, int textViewResourceId, List<SuggestedDestination> items) {
            super(context, resource, textViewResourceId, items);
        }
    }

}

package app.mortendahl.velib.ui.list;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import app.mortendahl.velib.Logger;
import app.mortendahl.velib.R;
import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.network.jcdecaux.VelibStation;
import app.mortendahl.velib.service.MonitoredVelibStationsChangedEvent;
import app.mortendahl.velib.service.VelibStationUpdatedEvent;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

public class StationListFragment extends Fragment implements AbsListView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private AbsListView mListView;

    private ListAdapter mAdapter;

    public static StationListFragment newInstance() {
        StationListFragment fragment = new StationListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    protected ArrayList<VelibStationListItem> items = new ArrayList<>();

    public StationListFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {}

        mAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, items);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(eventBusListener);
    }

    @Override
    public void onPause() {
        super.onResume();
        EventBus.getDefault().unregister(eventBusListener);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        int stationNumber = items.get(position).id;
        VelibStation station = VelibApplication.stationsMap.get(stationNumber);
//        Logger.debug(Logger.TAG_GUI, this, "deleting " + station.name);
//        VelibApplication.monitoredVelibStation.remove(stationNumber);
//
//        updateList();

        //GuidingService.setDestination(station);

        return true;
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    protected void updateList() {

        items.clear();

        for (Integer stationNumber : VelibApplication.monitoredVelibStation) {
            VelibStation station = VelibApplication.stationsMap.get(stationNumber);
            if (station != null) { items.add(VelibStationListItem.fromVelibStation(station)); }
        }

        mAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, items);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

    }

    private EventBusListener eventBusListener = new EventBusListener();


    private class EventBusListener {

        public void onEvent(VelibStationUpdatedEvent event) {
            Logger.debug(Logger.TAG_GUI, this, event.getClass().getSimpleName());
            updateList();
        }

        public void onEvent(MonitoredVelibStationsChangedEvent event) {
            updateList();
        }

    }

}

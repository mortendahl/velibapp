package app.mortendahl.velib.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import app.mortendahl.velib.Logger;
import app.mortendahl.velib.R;
import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.VelibDataStore;
import app.mortendahl.velib.service.data.RecentDestinationsUpdatedEvent;
import app.mortendahl.velib.service.data.SuggestedDestination;
import app.mortendahl.velib.service.guiding.GuidingService;
import de.greenrobot.event.EventBus;

public class RecentDestinationsFragment extends Fragment implements AbsListView.OnItemClickListener {

    public static RecentDestinationsFragment newInstance() {
        RecentDestinationsFragment fragment = new RecentDestinationsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private AbsListView listView;

    protected ArrayList<SuggestedDestination> items = new ArrayList<>();
    protected ArrayAdapter<SuggestedDestination> adapter;

    public RecentDestinationsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {}

        adapter = new SuggestedDestinationsAdapter(getActivity(), items);
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

        public void onEventMainThread(RecentDestinationsUpdatedEvent event) {
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

        // reload
        items.clear();
        items.addAll(VelibApplication.getDataStore().recentDestinations.getAll());

        // notify adapter about changes
        adapter.notifyDataSetChanged();

    }

    private class SuggestedDestinationsAdapter extends ArrayAdapter<SuggestedDestination> {

        public SuggestedDestinationsAdapter(Context context, List<SuggestedDestination> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            SuggestedDestination suggestedDestination = items.get(position);

            // check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_recentdestination, parent, false);
            }

            TextView addressTextView = (TextView) convertView.findViewById(R.id.address);
            TextView timestampTextView = (TextView) convertView.findViewById(R.id.timestamp);

            addressTextView.setText(suggestedDestination.getPrimaryAddressLine());
            timestampTextView.setText(formatTimestamp(suggestedDestination.timestamp));

            return convertView;
        }

        private String formatTimestamp(Long timestampInMilliseconds) {

            if (timestampInMilliseconds == null) { return ""; }

            return DateUtils.formatDateTime(getContext(), timestampInMilliseconds,
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE
                            | DateUtils.FORMAT_ABBREV_WEEKDAY | DateUtils.FORMAT_ABBREV_MONTH);

        }

    }

}

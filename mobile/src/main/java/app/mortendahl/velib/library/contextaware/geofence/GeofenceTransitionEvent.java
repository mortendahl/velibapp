package app.mortendahl.velib.library.contextaware.geofence;

import com.google.android.gms.location.Geofence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import app.mortendahl.velib.service.data.BaseEvent;

public class GeofenceTransitionEvent extends BaseEvent {

    public int transition;
    public ArrayList<String> fenceIds;

    @Override
    public String toString() {

        String allFenceIds = "";
        for (String fenceId : fenceIds) {
            allFenceIds += fenceId + " ";
        }

        return String.format("%s(%s, %s)", getClass().getSimpleName(), describeTransition(transition), allFenceIds);

    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();

        json.put("transition", describeTransition(transition));

        JSONArray jsonFenceIds = new JSONArray();
        for (String fenceId : fenceIds) { jsonFenceIds.put(fenceId); }
        json.put("fences", jsonFenceIds);

        return json;
    }

    public static String describeTransition(int transition) {
        return (transition == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter" :
               (transition == Geofence.GEOFENCE_TRANSITION_DWELL ? "dwell" :
               (transition == Geofence.GEOFENCE_TRANSITION_EXIT ? "exit" :
                "other")));
    }
}

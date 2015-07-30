package app.mortendahl.velib.library.contextaware.activity;

import com.google.android.gms.location.DetectedActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import app.mortendahl.velib.library.contextaware.BaseEvent;

public class ActivityUpdateEvent extends BaseEvent {

    private final HashMap<String, Integer> confidence = new HashMap<>();

    private ActivityUpdateEvent() {}

    public static ActivityUpdateEvent fromPlayActivities(List<DetectedActivity> activities) {
        ActivityUpdateEvent event = new ActivityUpdateEvent();
        for (DetectedActivity activity : activities) {
            int type = activity.getType();
            int confidence = activity.getConfidence();
            event.confidence.put(describeActivityType(type), confidence);
        }
        return event;
    }

    public int getConfidence(int type) {
        return confidence.get(describeActivityType(type));
    }

    public int getConfidence(String type) {
        return confidence.get(type);
    }

    @Override
    public String toString() {
        return String.format("%s(%d)", getClass().getSimpleName(), confidence.size());
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        for (Map.Entry<String, Integer> entry : confidence.entrySet()) {
            json.put(entry.getKey(), entry.getValue());
        }
        return json;
    }

    public static String describeActivityType(int type) {
        switch (type) {
            case DetectedActivity.IN_VEHICLE:
                return "vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "bicycle";
            case DetectedActivity.ON_FOOT:
                return "foot";
            case DetectedActivity.RUNNING:
                return "running";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.TILTING:
                return "tilting";
            case DetectedActivity.UNKNOWN:
                return "unknown";
            case DetectedActivity.WALKING:
                return "walking";
        }
        return String.format("other_%d", type);
    }

}

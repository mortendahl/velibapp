package app.mortendahl.velib.library.contextaware.activity;

import com.google.android.gms.location.DetectedActivity;

import org.json.JSONException;
import org.json.JSONObject;

import app.mortendahl.velib.library.eventbus.BaseEvent;

public class ActivityEvent extends BaseEvent {

    public int type;
    public int confidence;

    public ActivityEvent() {}

    public ActivityEvent(DetectedActivity activity) {
        this.type = activity.getType();
        this.confidence = activity.getConfidence();
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %d)", getClass().getSimpleName(), describeActivityType(type), confidence);
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("type", type);
        json.put("confidence", confidence);
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
        return "other";
    }

}

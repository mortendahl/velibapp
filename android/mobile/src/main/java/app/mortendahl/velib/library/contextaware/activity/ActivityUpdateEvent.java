package app.mortendahl.velib.library.contextaware.activity;

import com.google.android.gms.location.DetectedActivity;

import org.json.JSONException;
import org.json.JSONObject;

import app.mortendahl.velib.library.contextaware.BaseEvent;

public class ActivityUpdateEvent extends BaseEvent {

    public final int rawType;
    public final boolean inVehicle;
    public final boolean onBicycle;
    public final boolean onFoot;
    public final boolean still;

    public final int confidence;

    private ActivityUpdateEvent(int rawType, int confidence) {
        // type
        this.rawType = rawType;
        this.inVehicle = rawType == DetectedActivity.IN_VEHICLE;
        this.onBicycle = rawType == DetectedActivity.ON_BICYCLE;
        this.onFoot = rawType == DetectedActivity.ON_FOOT;
        this.still = rawType == DetectedActivity.STILL;
        // confidence
        this.confidence = confidence;
    }

    public static ActivityUpdateEvent fromPlayActivity(DetectedActivity activity) {
        ActivityUpdateEvent event = new ActivityUpdateEvent(activity.getType(), activity.getConfidence());
        return event;
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %d)", getClass().getSimpleName(), describeActivityType(rawType), confidence);
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("type", describeActivityType(rawType));
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
        return String.format("other(%d)", type);
    }

}

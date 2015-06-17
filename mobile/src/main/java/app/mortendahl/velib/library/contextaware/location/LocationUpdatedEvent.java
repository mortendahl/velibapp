package app.mortendahl.velib.library.contextaware.location;


import android.location.Location;

import org.json.JSONException;
import org.json.JSONObject;

import app.mortendahl.velib.library.eventbus.BaseEvent;

public class LocationUpdatedEvent extends BaseEvent {

    public final Location location;

    public LocationUpdatedEvent() {
        this.location = null;
    }

    public LocationUpdatedEvent(Location location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return  location != null
                ? String.format("%s(%f, %f, %f)", LocationUpdatedEvent.class.getSimpleName(), location.getLatitude(), location.getLongitude(), location.getAccuracy())
                : String.format("%s(null)", LocationUpdatedEvent.class.getSimpleName());
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        if (location != null) {
            json.put("latitude", location.getLatitude());
            json.put("longitude", location.getLongitude());
            json.put("accuracy", location.getAccuracy());
        }
        return json;
    }

}

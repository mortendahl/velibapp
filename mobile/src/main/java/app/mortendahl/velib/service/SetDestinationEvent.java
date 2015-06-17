package app.mortendahl.velib.service;

import org.json.JSONException;
import org.json.JSONObject;

import app.mortendahl.velib.library.eventbus.BaseEvent;
import app.mortendahl.velib.network.jcdecaux.Position;

public class SetDestinationEvent extends BaseEvent {

    public final Position destination;

    public SetDestinationEvent(Position destination) {
        this.destination = destination;
    }

    @Override
    public String toString() {
        return String.format("%s(%f, %f)", getClass().getSimpleName(), destination.latitude, destination.longitude);
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("latitude", destination.latitude);
        json.put("longitude", destination.longitude);
        return json;
    }

}

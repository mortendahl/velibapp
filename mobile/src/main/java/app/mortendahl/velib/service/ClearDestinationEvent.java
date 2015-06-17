package app.mortendahl.velib.service;

import org.json.JSONException;
import org.json.JSONObject;

import app.mortendahl.velib.library.eventbus.BaseEvent;

public class ClearDestinationEvent extends BaseEvent {

    public ClearDestinationEvent() {}

    @Override
    public String toString() {
        return String.format("%s()", getClass().getSimpleName());
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        return json;
    }

}

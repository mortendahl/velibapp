package app.mortendahl.velib.library.contextaware.power;

import org.json.JSONException;
import org.json.JSONObject;

import app.mortendahl.velib.library.contextaware.BaseEvent;

public class PowerUpdateEvent extends BaseEvent {

    public boolean connected;

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("connected", connected);
        return json;
    }

}

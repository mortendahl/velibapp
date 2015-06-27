package app.mortendahl.velib.library.contextaware;

import org.json.JSONException;
import org.json.JSONObject;

import app.mortendahl.velib.service.data.JsonFormattable;

public class BaseEvent implements JsonFormattable {

    public final long timestamp;

    public BaseEvent() {
        timestamp = System.currentTimeMillis();
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("class", getClass().getSimpleName());
        json.put("timestamp", timestamp);
        return json;
    }

}

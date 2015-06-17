package app.mortendahl.velib.library.eventbus;

import org.json.JSONException;
import org.json.JSONObject;

public class BaseEvent {

    public final long timestamp;

    public BaseEvent() {
        timestamp = System.currentTimeMillis();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("class", getClass().getSimpleName());
        json.put("timestamp", timestamp);
        return json;
    }

}

package app.mortendahl.velib.library.contextaware.connectivity;

import android.net.ConnectivityManager;

import org.json.JSONException;
import org.json.JSONObject;

import app.mortendahl.velib.library.contextaware.BaseEvent;

public class ConnectivityStabilisedEvent extends BaseEvent {

    public boolean connected;
    public Integer type;  // TODO use flags instead of (system) enum
    public String ssid;

    @Override
    public String toString() {

        if (connected) {
            if (type != null && type == ConnectivityManager.TYPE_WIFI) {
                return String.format("connectivity connected, wifi (%d), %s", type, ssid);
            } else {
                return String.format("connectivity connected, other (%d)", type);
            }
        } else {
            return String.format("connectivity disconnected");
        }

    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("connected", connected);
        json.put("type", type);
        json.put("ssid", ssid);
        return json;
    }

}

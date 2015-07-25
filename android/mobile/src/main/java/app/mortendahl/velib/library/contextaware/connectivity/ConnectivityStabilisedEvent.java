package app.mortendahl.velib.library.contextaware.connectivity;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.json.JSONException;
import org.json.JSONObject;

import app.mortendahl.velib.library.contextaware.BaseEvent;

public class ConnectivityStabilisedEvent extends BaseEvent {

    public boolean connected;
    public ConnectivityType type;
    public String ssid;

    @Override
    public String toString() {

        if (connected) {
            if (type != null && type == ConnectivityType.WIFI) {
                return String.format("connectivity connected, wifi (%s), %s", type, ssid);
            } else {
                return String.format("connectivity connected, other (%s)", type);
            }
        } else {
            return String.format("connectivity disconnected");
        }

    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        json.put("connected", connected);
        json.put("type", type.toString());
        json.put("ssid", ssid);
        return json;
    }

    public static ConnectivityType mapConnectivityType(NetworkInfo network) {

        if (network == null || !network.isConnectedOrConnecting()) {
            return ConnectivityType.NONE;
        } else if (network.getType() == ConnectivityManager.TYPE_WIFI) {
            return ConnectivityType.WIFI;
        } else {
            return ConnectivityType.OTHER;
        }

    }
}

package app.mortendahl.velib.service.data;

import org.json.JSONException;
import org.json.JSONObject;

public interface JsonFormattable {

    JSONObject toJson() throws JSONException;

}

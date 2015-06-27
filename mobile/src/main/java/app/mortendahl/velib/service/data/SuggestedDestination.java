package app.mortendahl.velib.service.data;

import android.location.Address;

import org.json.JSONException;
import org.json.JSONObject;

public class SuggestedDestination implements JsonFormattable {

    // required
    public final double weight;
    public final double latitude;
    public final double longitude;

    // optional
    public String addressPrimaryLine = null;
    public String addressPostalCode = null;
    public String addressLocality = null;
    public String addressCountryName = null;

    public void setAddress(Address address) {
        addressPrimaryLine = address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "";
        addressPostalCode = address.getPostalCode();
        addressLocality = address.getLocality();
        addressCountryName = address.getCountryName();
    }

    public SuggestedDestination(double weight, double latitude, double longitude) {
        this.weight = weight;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getPrimaryAddressLine() {
        return addressPrimaryLine;
    }

    public String getSecondaryAddressLine() {

        String lineTwo = null;
        if (addressPostalCode != null && !addressPostalCode.isEmpty()) {
            lineTwo = addressPostalCode;
        }
        if (addressLocality != null && !addressLocality.isEmpty()) {
            lineTwo = (lineTwo != null ? lineTwo + ", " + addressLocality : addressLocality);
        }
        if (addressCountryName != null && !addressCountryName.isEmpty()) {
            lineTwo = (lineTwo != null ? lineTwo + ", " + addressCountryName : addressCountryName);
        }

        return lineTwo;
    }

    @Override
    public String toString() {
        return addressPrimaryLine != null ? addressPrimaryLine : String.format("%f, %f", latitude, longitude);
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("class", getClass().getSimpleName());

        json.put("weight", weight);
        json.put("latitude", latitude);
        json.put("longitude", longitude);

        json.putOpt("address_primaryline", addressPrimaryLine);
        json.putOpt("address_postalcode", addressPostalCode);
        json.putOpt("address_locality", addressLocality);
        json.putOpt("address_countryname", addressCountryName);

        return json;
    }

    public static SuggestedDestination fromJson(JSONObject json) throws JSONException {

        double weight = json.getDouble("weight");
        double latitude = json.getDouble("latitude");
        double longitude = json.getDouble("longitude");

        SuggestedDestination dest = new SuggestedDestination(weight, latitude, longitude);
        dest.addressPrimaryLine = json.optString("address_primaryline", null);
        dest.addressPostalCode = json.optString("address_postalcode", null);
        dest.addressLocality = json.optString("address_locality", null);
        dest.addressCountryName = json.optString("address_countryname", null);

        return dest;

    }

}

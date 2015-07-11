package app.mortendahl.velib.service.data;

import android.location.Address;

import org.json.JSONException;
import org.json.JSONObject;

public class SuggestedDestination { //implements JsonFormattable {

    // required
    public double latitude;
    public double longitude;

    // optional
    public Double weight = null;
    public Long timestamp = null;
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

    public SuggestedDestination(double latitude, double longitude) {
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

}

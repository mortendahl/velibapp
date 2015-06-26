package app.mortendahl.velib.service.data;

import android.location.Address;

import app.mortendahl.velib.network.jcdecaux.Position;

public class SuggestedDestination {

    public final double weight;
    public final Position position;
    public Address address = null;

    public SuggestedDestination(double weight, double latitude, double longitude) {
        this.weight = weight;
        this.position = new Position(latitude, longitude);
    }

    public String getPrimaryAddressLine() {
        String lineOne = address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "";
        return lineOne;
    }

    public String getSecondaryAddressLine() {

        String postalCode = address.getPostalCode();
        String locality = address.getLocality();
        String countryName = address.getCountryName();
        String lineTwo = null;
        if (postalCode != null && !postalCode.isEmpty()) {
            lineTwo = postalCode;
        }
        if (locality != null && !locality.isEmpty()) {
            lineTwo = (lineTwo != null ? lineTwo + ", " + locality : locality);
        }
        if (countryName != null && !countryName.isEmpty()) {
            lineTwo = (lineTwo != null ? lineTwo + ", " + countryName : countryName);
        }

        return lineTwo;
    }

    @Override
    public String toString() {
        return address != null ? getPrimaryAddressLine() : String.format("%f, %f", position.latitude, position.longitude);
    }

}

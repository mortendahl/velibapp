package app.mortendahl.velib;

import com.google.android.gms.maps.model.LatLng;

public class Station {

    public String name;
    public int stands;
    public LatLng latlng;

    @Override
    public String toString() {
        return name + " (" + latlng + ")";
    }

}
package app.mortendahl.velib.service.data;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import app.mortendahl.velib.network.jcdecaux.Position;

public class PreviousDestination implements ClusterItem {

    public final Position position;
    public final long timestamp;
    private LatLng cachedLatLng = null;

    public PreviousDestination(Position position, long timestamp) {
        this.position = position;
        this.timestamp = timestamp;
    }

    @Override
    public LatLng getPosition() {
        if (cachedLatLng == null) {
            cachedLatLng = new LatLng(position.latitude, position.longitude);
        }
        return cachedLatLng;
    }

}

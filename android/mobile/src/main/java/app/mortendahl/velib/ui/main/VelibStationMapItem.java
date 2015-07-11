package app.mortendahl.velib.ui.main;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import app.mortendahl.velib.network.jcdecaux.VelibStation;

public class VelibStationMapItem implements ClusterItem {

    public int number;
    public String name;
    public LatLng position;
    public boolean open;
    public int availableStands;

    public static VelibStationMapItem fromStation(VelibStation station) {

        VelibStationMapItem item = new VelibStationMapItem();
        item.number = station.number;
        item.name = station.name;
        item.open = station.statusOpen;
        item.availableStands = station.availableStands;
        item.position = new LatLng(station.position.latitude, station.position.longitude);

        return item;
    }

//    @Override
//    public int hashCode() {
//        return number;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (o instanceof VelibStationMapItem) {
//            return this.number == ((VelibStationMapItem)o).number;
//        }
//        return false;
//    }

    @Override
    public LatLng getPosition() {
        return position;
    }

}

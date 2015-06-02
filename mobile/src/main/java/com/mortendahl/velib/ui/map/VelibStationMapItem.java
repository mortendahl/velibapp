package com.mortendahl.velib.ui.map;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.mortendahl.velib.network.jcdecaux.VelibStation;

public class VelibStationMapItem implements ClusterItem {

    public int number;
    public String name;
    public LatLng position;

    public static VelibStationMapItem fromStation(VelibStation station) {

        VelibStationMapItem item = new VelibStationMapItem();
        item.number = station.number;
        item.name = station.name;
        item.position = new LatLng(station.latitude, station.longitude);

        return item;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

}

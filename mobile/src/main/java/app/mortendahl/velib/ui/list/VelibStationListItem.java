package app.mortendahl.velib.ui.list;


import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.network.jcdecaux.VelibStation;

public class VelibStationListItem {

    public int id;

    public static VelibStationListItem fromVelibStation(VelibStation station) {

        VelibStationListItem item = new VelibStationListItem();
        item.id = station.number;

        return item;

    }

    @Override
    public String toString() {
        VelibStation station = VelibApplication.stationsMap.get(id);
        return id + " " + station.availableStands + " " + station.lastUpdate;
    }

}

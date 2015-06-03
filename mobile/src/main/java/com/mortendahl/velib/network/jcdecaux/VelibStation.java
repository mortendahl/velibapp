package com.mortendahl.velib.network.jcdecaux;

import org.json.JSONException;
import org.json.JSONObject;

public class VelibStation {

    public int number;
    public String name;
    public String address;
    public Position position;
    public boolean statusOpen;
    public String contract;
    public int bikeStands;
    public int availableStands;
    public int availableBikes;
    public long lastUpdate;

    public static VelibStation fromJSON(JSONObject json) throws JSONException {

        VelibStation station = new VelibStation();
        station.number = json.getInt("number");
        station.name = json.getString("name");
        station.address = json.getString("address");
        station.position = new Position(json.getJSONObject("position").getDouble("lat"), json.getJSONObject("position").getDouble("lng"));
        station.statusOpen = json.getString("status").equals("OPEN");
        station.contract = json.getString("contract_name");
        station.bikeStands = json.getInt("bike_stands");
        station.availableStands = json.getInt("available_bike_stands");
        station.availableBikes = json.getInt("available_bikes");
        station.lastUpdate = json.getInt("last_update");

        //{"banking":true,"bonus":true}

        return station;

    }

}

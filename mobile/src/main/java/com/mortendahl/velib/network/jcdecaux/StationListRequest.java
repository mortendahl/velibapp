package com.mortendahl.velib.network.jcdecaux;

import android.content.Context;

import com.mortendahl.velib.R;
import com.mortendahl.velib.VelibApplication;
import com.mortendahl.velib.network.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;

public class StationListRequest extends RestRequest<StationListRequest.StationResponse> {

    public StationListRequest() {

    }

    @Override
    public StationResponse call() throws Exception {

        Context context = VelibApplication.getCachedAppContext();
        URL url = new URL(context.getString(R.string.jcdecaux_endpoint_stations) + "&apiKey=" + context.getString(R.string.jcdecaux_key));

        JSONArray jsonStations = new JSONArray(server.sendHttpGetRequest(url));

        StationResponse response = new StationResponse();
        response.stations = new ArrayList<>();

        for (int i = 0; i < jsonStations.length(); i++) {
            JSONObject jsonStation = jsonStations.getJSONObject(i);
            VelibStation station = VelibStation.fromJSON(jsonStation);
            response.stations.add(station);
        }

        return response;

    }

    public static class StationResponse extends RestResponse {

        public ArrayList<VelibStation> stations;

    }

}

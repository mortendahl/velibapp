package com.mortendahl.velib.network.jcdecaux;

import org.json.*;

import java.util.*;

public class Contract {

    public String name;
    public String commercialName;
    public String countryCode;
    public List<String> cities;

    public static Contract fromJSON(JSONObject json) throws JSONException {

        Contract contract = new Contract();
        contract.name = json.getString("name");
        contract.commercialName = json.getString("commercial_name");
        contract.countryCode = json.getString("country_code");

        contract.cities = new ArrayList<>();
        JSONArray jsonCities = json.getJSONArray("cities");
        for (int i = 0; i < jsonCities.length(); i++) {
            contract.cities.add(jsonCities.getString(i));
        }

        return contract;

    }
}

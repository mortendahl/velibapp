package app.mortendahl.velib.network.jcdecaux;

import android.content.Context;

import app.mortendahl.velib.R;
import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.network.RestRequest;
import app.mortendahl.velib.network.RestResponse;

import org.json.*;

import java.net.URL;
import java.util.*;

public class ContractListRequest extends RestRequest<ContractListRequest.ContractListResponse> {

    public ContractListRequest() {

    }

    @Override
    public ContractListResponse call() throws Exception {

        Context context = VelibApplication.getCachedAppContext();
        URL url = new URL(context.getString(R.string.jcdecaux_endpoint_constractlist) + "?apiKey=" + context.getString(R.string.jcdecaux_key));

        JSONArray jsonContracts = new JSONArray(server.sendHttpGetRequest(url));

        ContractListResponse response = new ContractListResponse();

        response.contracts = new ArrayList<>();
        for (int i = 0; i < jsonContracts.length(); i++) {
            Contract contract = Contract.fromJSON(jsonContracts.getJSONObject(i));
            response.contracts.add(contract);
        }

        return response;

    }

    public static class ContractListResponse extends RestResponse {

        public List<Contract> contracts;

    }

}

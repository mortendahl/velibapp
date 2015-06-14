package app.mortendahl.velib.service;

import app.mortendahl.velib.network.RestRequest;
import app.mortendahl.velib.network.RestResponse;

public interface RestResponseHandler<T extends RestResponse> {

	public void onError(RestRequest<?> request, Exception e);
	
	public void onResponse(T response);

}

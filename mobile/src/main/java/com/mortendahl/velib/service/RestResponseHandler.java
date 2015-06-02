package com.mortendahl.velib.service;

import com.mortendahl.velib.network.RestRequest;
import com.mortendahl.velib.network.RestResponse;

public interface RestResponseHandler<T extends RestResponse> {

	public void onError(RestRequest<?> request, Exception e);
	
	public void onResponse(T response);

}

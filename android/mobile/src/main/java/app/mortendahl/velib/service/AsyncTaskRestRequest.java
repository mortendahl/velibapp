package app.mortendahl.velib.service;

import android.os.AsyncTask;

import app.mortendahl.velib.Logger;
import app.mortendahl.velib.network.RestRequest;
import app.mortendahl.velib.network.RestResponse;
import app.mortendahl.velib.network.ServerConnection;

public class AsyncTaskRestRequest<T extends RestRequest<?>> extends AsyncTask<Void, Void, RestResponse> {

	private final ServerConnection server;
	private final T request;
	private final RestResponseHandler responseHandler;
	
	private volatile Exception e = null;
	
	public AsyncTaskRestRequest(T request) {
		this.server = ServerConnection.sharedAuthInstance();
		this.request = request;
		this.responseHandler = null;
	}
	
	public AsyncTaskRestRequest(T request, RestResponseHandler responseHandler) {
		this.server = ServerConnection.sharedAuthInstance();
		this.request = request;
		this.responseHandler = responseHandler;
	}
	
	public AsyncTaskRestRequest(ServerConnection server, T request, RestResponseHandler responseHandler) {
		this.server = server;
		this.request = request;
		this.responseHandler = responseHandler;
	}
	
	@Override
	protected RestResponse doInBackground(Void... params) {
		
		try {
			if (server != null) { request.setServerConnection(server); }
			return request.call();
		}
		catch (Exception e) {
			Logger.error(Logger.TAG_REST, this, e.toString());
			this.e = e;
		}
		
		return null;
	}
	
	@Override
	protected void onPostExecute(RestResponse response) {
		if (response != null && e == null) {
			// success
			if (responseHandler != null) { responseHandler.onResponse(response); }
		} else {
			// failure
			if (responseHandler != null) { responseHandler.onError(request, e); }
		}
	}
	
}
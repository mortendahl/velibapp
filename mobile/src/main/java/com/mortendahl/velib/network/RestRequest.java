package com.mortendahl.velib.network;

import java.util.concurrent.Callable;

public abstract class RestRequest<T extends RestResponse> implements Callable<T> {

	public ServerConnection server;
	
	public void setServerConnection(ServerConnection server) {
		this.server = server;
	}
	
}

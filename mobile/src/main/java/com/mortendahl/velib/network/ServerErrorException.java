package com.mortendahl.velib.network;

import com.mortendahl.velib.Logger;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerErrorException extends Exception {
	
	public static final int DEFAULT_RESPONSE_CODE = -1;
	
	private int responseCode = DEFAULT_RESPONSE_CODE;
	private String responseBody;
	
	public int getResponseCode() {
		return responseCode;
	}
	
	public String getResponseBody() {
		return responseBody;
	}
	
	public String getResponseErrorMessage() {
		if (responseBody != null) {
			try {
				JSONObject json = new JSONObject(responseBody);
				return json.getString("error");
			} catch (JSONException e) {
				Logger.error(Logger.TAG_REST, this, e.toString());
			}
		}
		return null;
	}
	
	public ServerErrorException(int responseCode, String responseBody) {
		this.responseCode = responseCode;
		this.responseBody = responseBody;
	}
	
	@Override
	public String toString() {
		return super.toString() + ", code " + responseCode + ", body " + responseBody;
	}

}

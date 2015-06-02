package com.mortendahl.velib.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import com.mortendahl.velib.Logger;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;


public class ServerConnection {

	private static final boolean DEBUG = true;
	
	private static Context appContext;
	private static ServerConnection sharedInstance = null;
	
	public static void configure(Context appContext) {
		ServerConnection.appContext = appContext;
	}
	
	public static synchronized ServerConnection sharedAuthInstance() {
		if (sharedInstance == null) {
			sharedInstance = newInstance();
		}
		return sharedInstance;
	}
	
	public static void clearSharedAuthInstance() {
		sharedInstance = null;
	}
	
	public static ServerConnection newInstance() {
		
		// create user agent
		String userAgent = ServerConnection.compileUserAgent(appContext);
		
		// create conn object
		ServerConnection server = null;
		try { server = new ServerConnection(userAgent); } catch (MalformedURLException e) { throw new AssertionError("should not happen, " + e.toString()); }
		return server;

	}
		
	public static final String GET = "GET";
	public static final String POST = "POST";
	public static final String PUT = "PUT";
	public static final String DELETE = "DELETE";

	private final String userAgent;
	
	public ServerConnection(String userAgent) throws MalformedURLException {
		this.userAgent = userAgent;
		if (DEBUG) { Logger.debug(Logger.TAG_REST, this, "Creating server connection, user-agent: " + userAgent); }
	}
	
	public String sendHttpMessage(URL url, String method, String message) throws ServerErrorException, IOException {
		
		Logger.info( Logger.TAG_REST, this, String.format("%-4s %s, len(message):%d", method, url.toString(), (message != null ? message.length() : 0)) );
		if (DEBUG) { Logger.debug(Logger.TAG_REST, this, message); }
		
		// local variables needed in finally block
		HttpURLConnection conn = null;
		OutputStream output = null;
		BufferedReader input = null;
		int responseCode = ServerErrorException.DEFAULT_RESPONSE_CODE;
		String responseBody = null;
		
		// determine whether or not a body should be added to the request
		boolean attachBody = ( message != null && (method.equals("POST") || method.equals("PUT")) );
		
		try {
			
			// create new connection
			conn = (HttpURLConnection) url.openConnection();
			
			// set connection parameters
	        conn.setReadTimeout(10000);
	        conn.setConnectTimeout(15000);
			conn.setRequestMethod(method);
			conn.setDoInput(true);
			if (attachBody) {
				conn.setDoOutput(true);
			}
						
	        // set headers
			conn.setRequestProperty("User-Agent", userAgent);
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Accept-Language", Locale.getDefault().toString());
			
			if (attachBody) {
				conn.setRequestProperty("Content-Type", "application/json");
			}
			
			// connect
	        conn.connect();
	        
			if (attachBody) {
				// write body
				byte[] rawFormattedMessage = message.getBytes("UTF-8");	
				output = new BufferedOutputStream(conn.getOutputStream());
				output.write(rawFormattedMessage);
				output.flush();
			}
			
	        // wait for response
			//  - this may throw an exception in some cases, which we capture by checking against DEFAULT_RESPONSE_CODE in catch block
	        responseCode = conn.getResponseCode();

			// check if success or failure
	        if (responseCode == 200) {
	        
		        // success -> read response body
	        	input = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		        responseBody = emptyReader(input);
	        
	        } else {
	        
	        	// failure -> read error body
		        input = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
		        responseBody = emptyReader(input);
	        	
		        Logger.error(Logger.TAG_REST, this, "THROWING ServerErrorException, code:"+responseCode + ", body:"+responseBody);
	        	throw new ServerErrorException(responseCode, responseBody);
	        }
        
		}
		catch (IOException firstEx) {
			
			// error occurred
			//  - the library allows us to try a second time (without throwing an exception again)
			
			Logger.error(Logger.TAG_REST, this, firstEx.toString());
			
			
			// check if error was caused by getResponseCode()
			if (responseCode == ServerErrorException.DEFAULT_RESPONSE_CODE) {

				try {
					
					responseCode = conn.getResponseCode();  // shouldn't throw an exception the second time it's called, but doesn't seem to be correct
					
				}
				catch (IOException secondEx) {
					Logger.error(Logger.TAG_REST, this, secondEx.toString());
					// ignore second exception and continue to re-throw first one
					//  - there is no point in trying to read error body so we can't describe the error much better
					throw firstEx;
				}
				
			}
			
			if (responseBody == null || responseBody.isEmpty()) {
				
				// read error body
				input = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
		        responseBody = emptyReader(input);
		        
			}
			
			// we can now describe the error a bit better than with an IOException
			Logger.error(Logger.TAG_REST, this, "THROWING ServerErrorException, code:"+responseCode + ", body:"+responseBody);
			throw new ServerErrorException(responseCode, responseBody);
			
		}
		finally {
			
			if (output != null) { output.close(); }
			if (input != null) { input.close(); }
			if (conn != null) { conn.disconnect(); }
			
		}
        
		if (DEBUG) { Logger.debug(Logger.TAG_REST, this, "code:"+responseCode + ", body:\n" + responseBody); }
		return responseBody;
	}
	
	private String emptyReader(BufferedReader input) throws IOException {
		
		StringBuilder responseBodyBuilder = new StringBuilder();
		String newLine;
        while ( (newLine = input.readLine()) != null ) {
        	responseBodyBuilder.append(newLine);
        }
        return responseBodyBuilder.toString();
		
	}

	public String sendHttpGetRequest(URL url) throws ServerErrorException, IOException {
		return sendHttpMessage(url, GET, null);
	}

	public String sendHttpPutRequest(URL url, String message) throws ServerErrorException, IOException {
		return sendHttpMessage(url, PUT, message);
	}
	
	public String sendHttpPostRequest(URL url, String message) throws ServerErrorException, IOException {
		return sendHttpMessage(url, POST, message);
	}
	
	public String sendHttpDeleteMessage(URL url) throws ServerErrorException, IOException {
		return sendHttpMessage(url, DELETE, null);
	}
	
	private static String compileUserAgent(Context context) {
		return compileUserAgent(context.getPackageManager(), context.getPackageName());
	}
	
	private static String compileUserAgent(PackageManager packageManager, String packageName) {
		
		// compute current user agent (should not be saved in preferences)
		
		String appVersion = "UNKNOWN";
		
		try {
			
			PackageInfo pInfo = packageManager.getPackageInfo(packageName, 0);
			appVersion = pInfo.versionName;
			
		} catch (NameNotFoundException e) {
			// should never happen
			throw new AssertionError(e);
		}
		
		String mobileModel = Build.MANUFACTURER + " " + Build.MODEL;
		String userAgent = String.format("Velib %s; Android %s; %s", appVersion, Build.VERSION.RELEASE, mobileModel);
		
		return userAgent;
		
	}
		
}

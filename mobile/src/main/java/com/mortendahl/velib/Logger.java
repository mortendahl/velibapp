package com.mortendahl.velib;

import android.os.SystemClock;
import android.util.Log;

public final class Logger {

	public static final String TAG_REST		= "VelibRest";
	public static final String TAG_GUI		= "VelibGui";
	public static final String TAG_DATAMODEL= "VelibData";
	
	private static final boolean SHORTEN_MESSAGES = false;
	
	

	public static String format(String namedCaller, String message) {
		
		String threadName = Thread.currentThread().getName() + " (" + Thread.currentThread().getId() + ")";
		if (threadName.length() > 15) { threadName = threadName.substring(0, 15); }

		if (namedCaller.length() > 30) { namedCaller = namedCaller.substring(0, 30); }

		if (SHORTEN_MESSAGES && message.length() > 100) { message = message.substring(0, 100); }
		
		String formatted = String.format("%d ::: %-15s :: %-30s : %s", SystemClock.elapsedRealtime() / 1000, threadName, namedCaller, message);
		return formatted;
		
	}
	
	public static String format(Class<?> staticCaller, String message) {
		
		String caller = staticCaller.getSimpleName() + " (static)";
		return format(caller, message);
		
	}
	
	public static String format(Object instanceCaller, String message) {
		
		String caller = instanceCaller.getClass().getSimpleName() + " (" + instanceCaller.hashCode() + ")";
		return format(caller, message);
		
	}
	
	
	
	
	
	
	
	

	
	
	
	
	
	
	
	
	
	public static void debug(String tag, Class<?> staticCaller, String message) {
		Log.d(tag, format(staticCaller, message));
	}
	
	public static void debug(String tag, Object instanceCaller, String message) {
		Log.d(tag, format(instanceCaller, message));
	}
	
	public static void debug(String tag, String namedCaller, String message) {
		Log.d(tag, format(namedCaller, message));
	}
	
	public static void error(String tag, Class<?> staticCaller, String message) {
		Log.e(tag, format(staticCaller, message));
	}
	
	public static void error(String tag, Object instanceCaller, String message) {
		Log.e(tag, format(instanceCaller, message));
	}
	
	public static void error(String tag, String namedCaller, String message) {
		Log.e(tag, format(namedCaller, message));
	}
	
	public static void warn(String tag, Class<?> staticCaller, String message) {
		Log.w(tag, format(staticCaller, message));
	}
	
	public static void warn(String tag, Object instanceCaller, String message) {
		Log.w(tag, format(instanceCaller, message));
	}
	
	public static void warn(String tag, String namedCaller, String message) {
		Log.w(tag, format(namedCaller, message));
	}
	
	public static void info(String tag, Class<?> staticCaller, String message) {
		Log.i(tag, format(staticCaller, message));
	}
	
	public static void info(String tag, Object instanceCaller, String message) {
		Log.i(tag, format(instanceCaller, message));
	}
	
	public static void info(String tag, String namedCaller, String message) {
		Log.i(tag, format(namedCaller, message));
	}
	
}

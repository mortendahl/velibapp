package com.mortendahl.velib;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Vibrator;
import android.widget.Toast;

public final class UiHelper {
	
	private UiHelper() {}
	
	private static final ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
	
	public static void beep() {
		
		toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
		
	}
	
	// NOTE must be called on UI thread
	public static void toast(String message) {
		Context context = VelibApplication.getCachedAppContext();
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	public static void vibrate() {

		Context context = VelibApplication.getCachedAppContext();
		Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	
		// sleep, vibrate, sleep, vibrate, ...
		long[] pattern = {0, 100, 100, 100, 100, 100};
	
		// 0: repeat indefinitely
		// -1: vibration only once
		v.vibrate(pattern, -1);
		
	}
	
	public static void notif(Context appContext, int id, Class<? extends Activity> activity, String title, String text) {
		// build pending intent
		Intent notificationIntent = new Intent(appContext, activity);
		PendingIntent notificationPendingIntent = PendingIntent.getActivity(appContext, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		// .. and continue
		notif(appContext, id, notificationPendingIntent, title, text);
	}
	
	public static void notif(Context appContext, int id, PendingIntent notificationPendingIntent, String title, String text) {
		// build notification
		Notification notification = new Notification.Builder(appContext)
			.setSmallIcon(R.drawable.common_signin_btn_icon_dark)
			.setTicker(text)
			.setContentTitle(title)
			.setContentText(text)
			.setContentIntent(notificationPendingIntent)
			.getNotification();
		// show it
		NotificationManager notifMgr = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
		notifMgr.notify(id, notification);
	}

}

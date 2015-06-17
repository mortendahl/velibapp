package app.mortendahl.velib.library.contextaware.connectivity;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;

import app.mortendahl.velib.VelibApplication;
import de.greenrobot.event.EventBus;

public class ConnectivityReceiver extends BroadcastReceiver {

    /**
     * Delay before we declare connectivity stabilised.
     */
    private static final long DELAYED_CALLBACK_INTERVAL = 1000 * 10;  // in miliseconds

    /**
     * Called every time there's a connectivity change.
     */
    protected void onConnectivityChange(Context context, Intent intent) {}

    /**
     * Called when we suspect that connectivity has stabilised, i.e. not changed for a while.
     * Called regardless of whether or not we already had connectivity.
     */
    protected void onStabilisedConnectivity(Context context, NetworkInfo activeNetwork) {

        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {

            //
            // is connected
            //

            if (ConnectivityManager.TYPE_WIFI == activeNetwork.getType()) {

                //
                // connected to WiFi
                //

                String currentSsid = getCurrentWifiSsid();

                ConnectivityEvent event = new ConnectivityEvent();
                event.connected = true;
                event.type = activeNetwork.getType();
                event.ssid = currentSsid;

                EventBus.getDefault().post(event);

            } else {

                //
                // connected to something other than WiFi
                //

                ConnectivityEvent event = new ConnectivityEvent();
                event.connected = true;
                event.type = activeNetwork.getType();
                event.ssid = null;

                EventBus.getDefault().post(event);

            }

        } else {

            //
            // is not connected
            //

            ConnectivityEvent event = new ConnectivityEvent();
            event.connected = false;
            event.type = null;
            event.ssid = null;

            EventBus.getDefault().post(event);

        }

    }

    public static String getCurrentWifiSsid() {
        WifiManager wifiManager = (WifiManager) VelibApplication.getCachedAppContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getSSID();
    }

//    public static String getReactiveWifi() {
//
//        DB snappydb = null;
//
//        try {
//
//            snappydb = DBFactory.open(MyApplication.appContext);
//            return snappydb.get("reactive_wifi");
//
//        }
//        catch (SnappydbException e) {
//            Log.e(MyApplication.TAG, "error getting reactive wifi, " + e.toString());
//        }
//        finally {
//            if (snappydb != null) {
//                try { snappydb.close(); }
//                catch (SnappydbException e) {}
//            }
//        }
//
//        return null;
//
//    }
//
//    public static void setReactiveWifi(String ssid) {
//
//        DB snappydb = null;
//
//        try {
//
//            snappydb = DBFactory.open(MyApplication.appContext);
//            snappydb.put("reactive_wifi", ssid);
//
//        }
//        catch (SnappydbException e) {
//            Log.e(MyApplication.TAG, "error saving reactive wifi, " + e.toString());
//        }
//        finally {
//            if (snappydb != null) {
//                try { snappydb.close(); }
//                catch (SnappydbException e) {}
//            }
//        }
//
//    }
//
//    public static void clearReactiveWifi() {
//
//        DB snappydb = null;
//
//        try {
//
//            snappydb = DBFactory.open(MyApplication.appContext);
//            snappydb.del("reactive_wifi");
//
//        }
//        catch (SnappydbException e) {
//            Log.e(MyApplication.TAG, "error clearing reactive wifi, " + e.toString());
//        }
//        finally {
//            if (snappydb != null) {
//                try { snappydb.close(); }
//                catch (SnappydbException e) {}
//            }
//        }
//
//    }


    /***********************
     *
     *  	Plumbing
     *
     ***********************/

    private static final String ACTION_DELAYED_CALLBACK = "action_delayed_callback";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) { return; }

        String action = intent.getAction();
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {

            // fire event handler
            onConnectivityChange(context, intent);

            // install a delayed callback alarm in case connectivity has not settled yet
            //  - this will override nay previous delayed callback alarms
            installDelayedCallback(context);

        } else if (ACTION_DELAYED_CALLBACK.equals(action)) {

            // hope that connectivity has stabilised

            // get current active network
            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();

            // fire event handlers
            onStabilisedConnectivity(context, activeNetwork);

        } else {

            // unhandled action

//            Log.e(MyApplication.TAG, "unhandled action, " + action);

        }

    }

    private static PendingIntent getPendingIntentForDelayedCallback(Context context) {

        Intent intent = new Intent(ACTION_DELAYED_CALLBACK, null, context, ConnectivityReceiver.class);
        //return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);  // TODO try with this
        return PendingIntent.getBroadcast(context, 0, intent, 0);

    }

    private static void installDelayedCallback(Context context) {

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getPendingIntentForDelayedCallback(context);

        // cancel any previous wake-up alarms
        alarmMgr.cancel(pendingIntent);

        // install new alarm
        alarmMgr.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + DELAYED_CALLBACK_INTERVAL,
                pendingIntent);

    }

}
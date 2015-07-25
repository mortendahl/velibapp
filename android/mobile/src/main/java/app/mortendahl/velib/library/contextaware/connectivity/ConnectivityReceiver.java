package app.mortendahl.velib.library.contextaware.connectivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;

import app.mortendahl.velib.library.background.BaseBroadcastReceiver;
import app.mortendahl.velib.library.background.BroadcastReceiverActionHandler;
import app.mortendahl.velib.library.contextaware.ContextAwareApplication;
import de.greenrobot.event.EventBus;

public class ConnectivityReceiver extends BaseBroadcastReceiver {

    /**
     * Delay before we declare stabilised connectivity.
     */
    protected static final long DELAYED_CALLBACK_INTERVAL = 1000 * 10;  // in miliseconds


    public ConnectivityReceiver() {
        setActionHandlers(
                new ConnectivityHandler(),
                new DelayedCallbackHandler()
        );
    }

    private static class ConnectivityHandler extends BroadcastReceiverActionHandler {

        @Override
        public String getAction() {
            return ConnectivityManager.CONNECTIVITY_ACTION;
        }

        @Override
        public void handle(Context context, Intent intent) {

            // install a delayed callback alarm in case connectivity has not settled yet
            //  - this will override nay previous delayed callback alarms
            installDelayedCallback(context);

            ContextAwareApplication app = (ContextAwareApplication) context.getApplicationContext();
            ConnectivityChangeEvent event = new ConnectivityChangeEvent();
            app.getContextAwareHandler().onConnectivityChange(event);

        }

        private void installDelayedCallback(Context context) {

            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = DelayedCallbackHandler.getPendingIntent(context);

            // cancel any previous wake-up alarms
            alarmMgr.cancel(pendingIntent);

            // install new alarm
            alarmMgr.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + DELAYED_CALLBACK_INTERVAL,
                    pendingIntent);

        }

    }

    private static class DelayedCallbackHandler extends BroadcastReceiverActionHandler {

        protected static final String ACTION_DELAYED_CALLBACK = "delayed_callback";

        protected static PendingIntent getPendingIntent(Context context) {
            Intent intent = new Intent(ACTION_DELAYED_CALLBACK, null, context, ConnectivityReceiver.class);
            //return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);  // TODO try with this
            return PendingIntent.getBroadcast(context, 0, intent, 0);
        }

        @Override
        public String getAction() {
            return ACTION_DELAYED_CALLBACK;
        }

        @Override
        public void handle(Context context, Intent intent) {

            // hope that connectivity has stabilised

            // get current active network
            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();

            ContextAwareApplication app = (ContextAwareApplication) context.getApplicationContext();

            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {

                //
                // is connected
                //

                if (ConnectivityManager.TYPE_WIFI == activeNetwork.getType()) {

                    //
                    // connected to WiFi
                    //

                    String currentSsid = getCurrentWifiSsid(context);

                    ConnectivityStabilisedEvent event = new ConnectivityStabilisedEvent();
                    event.connected = true;
                    event.type = ConnectivityStabilisedEvent.mapConnectivityType(activeNetwork);
                    event.ssid = currentSsid;

                    EventBus.getDefault().post(event);

                    app.getContextAwareHandler().onConnectivityStabilised(event);

                } else {

                    //
                    // connected to something other than WiFi
                    //

                    ConnectivityStabilisedEvent event = new ConnectivityStabilisedEvent();
                    event.connected = true;
                    event.type = ConnectivityStabilisedEvent.mapConnectivityType(activeNetwork);
                    event.ssid = null;

                    EventBus.getDefault().post(event);

                    app.getContextAwareHandler().onConnectivityStabilised(event);

                }

            } else {

                //
                // is not connected
                //

                ConnectivityStabilisedEvent event = new ConnectivityStabilisedEvent();
                event.connected = false;
                event.type = ConnectivityType.NONE;
                event.ssid = null;

                EventBus.getDefault().post(event);

                app.getContextAwareHandler().onConnectivityStabilised(event);

            }

        }

        private String getCurrentWifiSsid(Context context) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            return wifiInfo.getSSID();
        }

    }

}
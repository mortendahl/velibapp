package app.mortendahl.velib.library.contextaware.power;

import android.content.Context;
import android.content.Intent;

import app.mortendahl.velib.library.background.ActionHandler;
import app.mortendahl.velib.library.background.BaseBroadcastReceiver;
import app.mortendahl.velib.library.contextaware.ContextAwareApplication;

public class PowerReceiver extends BaseBroadcastReceiver {

    public PowerReceiver() {
        setActionHandlers(
                new PowerConnectedHandler(),
                new PowerDisconnectedHandler()
        );
    }

    private static class PowerConnectedHandler extends ActionHandler {

        @Override
        public String getAction() {
            return Intent.ACTION_POWER_CONNECTED;
        }

        @Override
        public void handle(Context context, Intent intent) {

            PowerUpdateEvent event = new PowerUpdateEvent();
            event.connected = true;

            ContextAwareApplication app = (ContextAwareApplication) context.getApplicationContext();
            app.getContextAwareHandler().onPowerUpdate(event);

        }

    }

    private static class PowerDisconnectedHandler extends ActionHandler {

        @Override
        public String getAction() {
            return Intent.ACTION_POWER_DISCONNECTED;
        }

        @Override
        public void handle(Context context, Intent intent) {

            PowerUpdateEvent event = new PowerUpdateEvent();
            event.connected = false;

            ContextAwareApplication app = (ContextAwareApplication) context.getApplicationContext();
            app.getContextAwareHandler().onPowerUpdate(event);

        }

    }

}
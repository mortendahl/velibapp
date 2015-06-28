package app.mortendahl.velib.library.background;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.HashMap;

import app.mortendahl.velib.Logger;

public abstract class BaseService extends Service {

    private HashMap<String, ActionHandler> actionMap;

    protected void setActionHandlers(ActionHandler... actionHandlers) {
        actionMap = new HashMap<>();
        for (ActionHandler actionHandler : actionHandlers) {
            actionMap.put(actionHandler.getAction(), actionHandler);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.debug(Logger.TAG_SERVICE, this, "creating");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean keepRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Boolean newKeepRunning = null;

        String action = (intent != null ? intent.getAction() : null);
        if (action != null) {
            ActionHandler handler = actionMap.get(action);
            newKeepRunning = handler.handleSticky(this, intent);
        }

        if (newKeepRunning != null && keepRunning != newKeepRunning) {
            keepRunning = newKeepRunning;
//            onKeepRunningChanged(keepRunning);
            if (keepRunning) {
                onEnteringSticky();
            } else {
                onLeavingSticky();
            }
        }

        if (!keepRunning) {
            Logger.debug(Logger.TAG_SERVICE, this, "stopping");
            stopForeground(true);
            stopSelf();
        }

        return keepRunning ? START_STICKY : START_NOT_STICKY;
    }

//    protected void onKeepRunningChanged(boolean keepRunning) {}

    protected void onEnteringSticky() {}

    protected void onLeavingSticky() {}

}

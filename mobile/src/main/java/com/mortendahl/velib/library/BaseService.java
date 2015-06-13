package com.mortendahl.velib.library;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import java.util.HashMap;

public class BaseService extends Service {

    private HashMap<String, ActionHandler> actionMap;

    protected void setActionHandlers(ActionHandler... actionHandlers) {
        actionMap = new HashMap<>();
        for (ActionHandler actionHandler : actionHandlers) {
            actionMap.put(actionHandler.getAction(), actionHandler);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        boolean keepRunning = false;

        String action = (intent != null ? intent.getAction() : null);
        if (action != null) {
            ActionHandler handler = actionMap.get(action);
            keepRunning = handler.handleSticky(this, intent);
        }

        onIntentHandled(keepRunning);

        if (keepRunning) {
            return START_STICKY;

        } else {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;

        }
    }

    protected void onIntentHandled(boolean keepRunning) {}

}

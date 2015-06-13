package com.mortendahl.velib.library;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.HashMap;

public class BaseIntentService extends IntentService {

    private HashMap<String, ActionHandler> actionMap;

    public BaseIntentService() {
        super(BaseIntentService.class.getSimpleName());
    }

    public BaseIntentService(String name) {
        super(name);
    }

    protected void setActionHandlers(ActionHandler... actionHandlers) {
        actionMap = new HashMap<>();
        for (ActionHandler actionHandler : actionHandlers) {
            actionMap.put(actionHandler.getAction(), actionHandler);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        String action = (intent != null ? intent.getAction() : null);
        if (action == null) { return; }

        ActionHandler handler = actionMap.get(action);
        handler.handle(this, intent);

    }

}

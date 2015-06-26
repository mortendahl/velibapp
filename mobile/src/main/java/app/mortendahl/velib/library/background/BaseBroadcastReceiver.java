package app.mortendahl.velib.library.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.HashMap;

public abstract class BaseBroadcastReceiver extends BroadcastReceiver {

    private HashMap<String, ActionHandler> actionMap;

    protected void setActionHandlers(ActionHandler... actionHandlers) {
        actionMap = new HashMap<>();
        for (ActionHandler actionHandler : actionHandlers) {
            actionMap.put(actionHandler.getAction(), actionHandler);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = (intent != null ? intent.getAction() : null);
        if (action == null) { return; }

        ActionHandler handler = actionMap.get(action);
        if (handler == null) { return; }

        handler.handle(context, intent);

    }

}

package app.mortendahl.velib.library.background;

import android.app.IntentService;
import android.content.Intent;

import java.util.HashMap;

public abstract class BaseIntentService extends IntentService {

    private volatile HashMap<String, IntentServiceActionHandler> actionMap;

    public BaseIntentService() {
        super(BaseIntentService.class.getSimpleName());
    }

    public BaseIntentService(String name) {
        super(name);
    }

    protected void setActionHandlers(IntentServiceActionHandler... actionHandlers) {
        actionMap = new HashMap<>();
        for (IntentServiceActionHandler actionHandler : actionHandlers) {
            actionMap.put(actionHandler.getAction(), actionHandler);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        String action = (intent != null ? intent.getAction() : null);
        if (action == null) { return; }

        IntentServiceActionHandler handler = actionMap.get(action);
        handler.handle(this, intent);

    }

}

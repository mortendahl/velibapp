package app.mortendahl.velib.library.background;

import android.content.Context;
import android.content.Intent;

public abstract class ActionHandler {

    public abstract String getAction();

    public Boolean handleSticky(Context context, Intent intent) {
        handle(context, intent);
        return false;
    }

    public void handle(Context context, Intent intent) {
        throw new UnsupportedOperationException("must override handle");
    }

}

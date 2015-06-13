package com.mortendahl.velib.library;

import android.content.Context;
import android.content.Intent;

public abstract class ActionHandler {

    public abstract String getAction();

    public boolean handleSticky(Context context, Intent intent) {
        handle(context, intent);
        return false;
    }

    public void handle(Context context, Intent intent) {
        throw new UnsupportedOperationException("must override handle");
    }

}

package app.mortendahl.velib.library.background;

import android.content.Context;
import android.content.Intent;

public abstract class BroadcastReceiverActionHandler {

    public abstract String getAction();

    public abstract void handle(Context context, Intent intent);

}

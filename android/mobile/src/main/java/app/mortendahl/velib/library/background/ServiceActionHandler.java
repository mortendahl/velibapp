package app.mortendahl.velib.library.background;

import android.content.Context;
import android.content.Intent;

public abstract class ServiceActionHandler {

    public abstract String getAction();

    public abstract Boolean handle(Context context, Intent intent);

}

package app.mortendahl.velib.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import app.mortendahl.velib.Logger;
import app.mortendahl.velib.R;
import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.library.background.ActionHandler;
import app.mortendahl.velib.library.background.BaseService;

public class StationUpdatorService extends BaseService {

    protected Updator updator;
    protected int referenceCount = 0;

    public StationUpdatorService() {
        setActionHandlers(
                new SetActiveHandler(this)
        );
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updator = new Updator();
    }

    protected class Updator implements Runnable {

        private final Handler handler;
        private boolean started = false;

        public Updator() {
            handler = new Handler();
        }

        public void start() {
            if (started) { return; }
            started = true;
            handler.postDelayed(this, 0);
        }

        public void stop() {
            started = false;
            handler.removeCallbacks(this);
        }

        @Override
        public void run() {
            VelibApplication.reloadStations();
            handler.postDelayed(this, 5000);
        }

    }


    public static final SetActiveHandler.Invoker updatesAction = new SetActiveHandler.Invoker();

    public static class SetActiveHandler extends ActionHandler {

        public static final String ACTION = "set_active";
        public static final String KEY_DIRECTION = "direction";

        @Override
        public String getAction() {
            return ACTION;
        }

        public static class Invoker {
            public void request(Context context) {
                Intent intent = new Intent(context, StationUpdatorService.class);
                intent.setAction(ACTION);
                intent.putExtra(KEY_DIRECTION, 1);
                context.startService(intent);
            }

            public void remove(Context context) {
                Intent intent = new Intent(context, StationUpdatorService.class);
                intent.setAction(ACTION);
                intent.putExtra(KEY_DIRECTION, -1);
                context.startService(intent);
            }
        }

        protected final StationUpdatorService state;

        public SetActiveHandler(StationUpdatorService state) {
            this.state = state;
        }


        @Override
        public Boolean handleSticky(Context context, Intent intent) {

            Bundle bundle = intent.getExtras();
            if (!bundle.containsKey(KEY_DIRECTION)) { return null; }

            int direction = bundle.getInt(KEY_DIRECTION);
            state.referenceCount += direction;

            if (state.referenceCount > 0) {
                state.updator.start();
                return true;
            } else {
                state.updator.stop();
                return false;
            }
        }
    }

}

package app.mortendahl.velib.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import app.mortendahl.velib.Logger;
import app.mortendahl.velib.R;
import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.library.background.ActionHandler;
import app.mortendahl.velib.library.background.BaseService;

public class StationUpdatorService extends BaseService {

    protected Updator updator;
    protected LinkedHashSet<String> requester = new LinkedHashSet<>();

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
            Logger.debug(Logger.TAG_SERVICE, this, "started updator");
        }

        public void stop() {
            started = false;
            handler.removeCallbacks(this);
            Logger.debug(Logger.TAG_SERVICE, this, "stopped updator");
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
        public static final String KEY_TAG = "tag";
        public static final String KEY_ACTION = "action";
        protected static final int ACTION_REMOVE = 0;
        protected static final int ACTION_REQUEST = 1;

        @Override
        public String getAction() {
            return ACTION;
        }

        public static class Invoker {
            public void request(Context context, String tag) {
                Logger.debug(Logger.TAG_SERVICE, this, "request, " + tag);
                Intent intent = new Intent(context, StationUpdatorService.class);
                intent.setAction(ACTION);
                intent.putExtra(KEY_TAG, tag);
                intent.putExtra(KEY_ACTION, ACTION_REQUEST);
                context.startService(intent);
            }

            public void remove(Context context, String tag) {
                Logger.debug(Logger.TAG_SERVICE, this, "remove, " + tag);
                Intent intent = new Intent(context, StationUpdatorService.class);
                intent.setAction(ACTION);
                intent.putExtra(KEY_TAG, tag);
                intent.putExtra(KEY_ACTION, ACTION_REMOVE);
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
            if (!bundle.containsKey(KEY_TAG)) { return null; }
            if (!bundle.containsKey(KEY_ACTION)) { return null; }

            String tag = bundle.getString(KEY_TAG);
            int action = bundle.getInt(KEY_ACTION);

            if (action == ACTION_REQUEST) {
                state.requester.add(tag);
            } else {
                state.requester.remove(tag);
            }

            if (!state.requester.isEmpty()) {
                state.updator.start();
                return true;
            } else {
                state.updator.stop();
                return false;
            }
        }
    }

}

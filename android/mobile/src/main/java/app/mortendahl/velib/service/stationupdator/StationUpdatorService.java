package app.mortendahl.velib.service.stationupdator;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

import app.mortendahl.velib.Logger;
import app.mortendahl.velib.VelibApplication;
import app.mortendahl.velib.library.background.BaseService;
import app.mortendahl.velib.library.background.ServiceActionHandler;
import app.mortendahl.velib.library.background.WakeLockManager;
import app.mortendahl.velib.network.RestRequest;
import app.mortendahl.velib.network.jcdecaux.StationListRequest;
import app.mortendahl.velib.network.jcdecaux.VelibStation;
import app.mortendahl.velib.service.AsyncTaskRestRequest;
import app.mortendahl.velib.service.RestResponseHandler;
import de.greenrobot.event.EventBus;

public class StationUpdatorService extends BaseService {

    protected static WakeLockManager wakeLockManager = new WakeLockManager();

    protected Updator updator;
    protected LinkedHashSet<String> requester = new LinkedHashSet<>();

    public StationUpdatorService() {
        setActionHandlers(
                new SetActiveHandler.Handler(this)
        );
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updator = new Updator();
    }

    protected class Updator implements Runnable {

        private Handler handler = null;
        private boolean started = false;

        public Updator() {}

        public void start() {
            if (started) { return; }
            started = true;
            if (handler == null) { handler = new Handler(); }
            handler.postDelayed(this, 0);
            Logger.debug(Logger.TAG_SYSTEM, this, "started updator");
        }

        public void stop() {
            started = false;
            handler.removeCallbacks(this);
            Logger.debug(Logger.TAG_SYSTEM, this, "stopped updator");
        }

        @Override
        public void run() {
            reloadStations();
            handler.postDelayed(this, 15000);
        }

        private StationListRequest currentRequest = null;

        private void reloadStations() {

            Logger.debug(Logger.TAG_SYSTEM, VelibApplication.class, "reloadStations" + (currentRequest!=null?", skipping" :""));
            if (currentRequest != null) { return; }  // wait for any current request to finish

            currentRequest = new StationListRequest();

            new AsyncTaskRestRequest<>(currentRequest, new RestResponseHandler<StationListRequest.StationResponse>() {

                @Override
                public void onError(RestRequest<?> request, Exception e) {
                    currentRequest = null;
                    Logger.debug(Logger.TAG_SYSTEM, this, "reloadStations, onError, " + e.toString());
                }

                @Override
                public void onResponse(StationListRequest.StationResponse response) {
                    currentRequest = null;
                    updateStations(response.stations);
                    Logger.debug(Logger.TAG_SYSTEM, this, "reloadStations, onResponse");
                }

            }).execute();

        }

        protected void updateStations(Collection<VelibStation> stations) {
            boolean added = false;

            Map<Integer, VelibStation> stationsMap = VelibApplication.getSessionStore().stationsMap;

            for (VelibStation station : stations) {
                Object previousMapping = stationsMap.put(station.number, station);
                added = added || previousMapping == null;
            }

            if (added) {
                EventBus.getDefault().post(new VelibStationsChangedEvent());
            } else {
                EventBus.getDefault().post(new VelibStationUpdatedEvent());
            }

        }

    }


    public static final SetActiveHandler.Invoker updatesAction = new SetActiveHandler.Invoker();

    public static class SetActiveHandler {

        public static final String ACTION = "set_active";
        public static final String KEY_TAG = "tag";
        public static final String KEY_ACTION = "action";
        protected static final int ACTION_REMOVE = 0;
        protected static final int ACTION_REQUEST = 1;

        public static class Invoker {
            public void request(Context context, String tag) {
                Logger.debug(Logger.TAG_SERVICE, this, "request, " + tag);

                wakeLockManager.acquireWakeLock(context);

                Intent intent = new Intent(context, StationUpdatorService.class);
                intent.setAction(ACTION);
                intent.putExtra(KEY_TAG, tag);
                intent.putExtra(KEY_ACTION, ACTION_REQUEST);
                context.startService(intent);
            }

            public void remove(Context context, String tag) {
                Logger.debug(Logger.TAG_SERVICE, this, "remove, " + tag);

                wakeLockManager.acquireWakeLock(context);

                Intent intent = new Intent(context, StationUpdatorService.class);
                intent.setAction(ACTION);
                intent.putExtra(KEY_TAG, tag);
                intent.putExtra(KEY_ACTION, ACTION_REMOVE);
                context.startService(intent);
            }
        }

        public static class Handler extends ServiceActionHandler {

            @Override
            public String getAction() {
                return ACTION;
            }

            protected final StationUpdatorService state;

            public Handler(StationUpdatorService state) {
                this.state = state;
            }

            @Override
            public Boolean handle(Context context, Intent intent) {

                Bundle bundle = intent.getExtras();
                if (!bundle.containsKey(KEY_TAG)) { throw new AssertionError("no KEY_TAG"); }
                if (!bundle.containsKey(KEY_ACTION)) { throw new AssertionError("no KEY_ACTION"); }

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
                    wakeLockManager.releaseWakelock();
                    return false;
                }
            }

        }

    }

}

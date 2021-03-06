package app.mortendahl.velib;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;

import app.mortendahl.velib.network.jcdecaux.VelibStation;
import app.mortendahl.velib.service.data.BaseStoredList;
import app.mortendahl.velib.service.data.GsonStoredList;
import app.mortendahl.velib.service.data.SuggestedDestination;

public class VelibDataStore {

    public final BaseStoredList<VelibStation> stations = new GsonStoredList<VelibStation>("cache_stations") {
        @Override
        protected Type getType() {
            return VelibStation.class;
        }
    };

    public final BaseStoredList<SuggestedDestination> predictedDestinations = new GsonStoredList<SuggestedDestination>("cache_suggested_destinations") {
        @Override
        protected Type getType() {
            return SuggestedDestination.class;
        }
    };

    public final BaseStoredList<SuggestedDestination> recentDestinations = new GsonStoredList<SuggestedDestination>("cache_recent_destinations") {
        @Override
        protected Type getType() {
            return SuggestedDestination.class;
            //return new TypeToken<SuggestedDestination>() {}.getType();
        }
    };

}

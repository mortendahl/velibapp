package app.mortendahl.velib.service.data;

import java.lang.reflect.Type;

public class DataManager {

    public static final BaseStoredList<SuggestedDestination> suggestedDestinations = new GsonStoredList<SuggestedDestination>("cache_suggested_destinations") {
        @Override
        protected Type getType() {
            return SuggestedDestination.class;
            //return new TypeToken<SuggestedDestination>() {}.getType();
        }
    };

    public static final BaseStoredList<SuggestedDestination> recentDestinations = new GsonStoredList<SuggestedDestination>("cache_recent_destinations") {
        @Override
        protected Type getType() {
            return SuggestedDestination.class;
            //return new TypeToken<SuggestedDestination>() {}.getType();
        }
    };

}

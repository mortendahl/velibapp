package app.mortendahl.velib.library.contextaware;

import app.mortendahl.velib.library.contextaware.activity.ActivityUpdateEvent;
import app.mortendahl.velib.library.contextaware.connectivity.ConnectivityChangeEvent;
import app.mortendahl.velib.library.contextaware.connectivity.ConnectivityStabilisedEvent;
import app.mortendahl.velib.library.contextaware.geofence.GeofenceTransitionEvent;
import app.mortendahl.velib.library.contextaware.location.LocationUpdateEvent;
import app.mortendahl.velib.library.contextaware.power.PowerUpdateEvent;

public interface ContextAwareHandler {

    void onPowerUpdate(PowerUpdateEvent event);

    void onActivityUpdate(ActivityUpdateEvent event);

    void onLocationUpdate(LocationUpdateEvent event);

    void onGeofenceTransition(GeofenceTransitionEvent event);

    /**
     * Called every time there's a connectivity change.
     */
    void onConnectivityChange(ConnectivityChangeEvent event);

    /**
     * Called when we suspect that connectivity has stabilised, i.e. not changed for a while.
     * Called regardless of whether or not we already had connectivity.
     */
    void onConnectivityStabilised(ConnectivityStabilisedEvent event);

}

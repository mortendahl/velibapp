package app.mortendahl.velib.library.contextaware;

import app.mortendahl.velib.library.contextaware.activity.ActivityUpdateEvent;
import app.mortendahl.velib.library.contextaware.connectivity.ConnectivityChangeEvent;
import app.mortendahl.velib.library.contextaware.connectivity.ConnectivityStabilisedEvent;
import app.mortendahl.velib.library.contextaware.geofence.GeofenceTransitionEvent;
import app.mortendahl.velib.library.contextaware.location.LocationUpdateEvent;
import app.mortendahl.velib.library.contextaware.power.PowerUpdateEvent;

// TODO class should be abstract, but let's use type system for now to check that we capture all events
public class BaseContextAwareHandler implements ContextAwareHandler {

    @Override
    public void onActivityUpdate(ActivityUpdateEvent event) {}

    @Override
    public void onLocationUpdate(LocationUpdateEvent event) {}

    @Override
    public void onGeofenceTransition(GeofenceTransitionEvent event) {}

    @Override
    public void onConnectivityChange(ConnectivityChangeEvent event) {}

    @Override
    public void onConnectivityStabilised(ConnectivityStabilisedEvent event) {}

    @Override
    public void onPowerUpdate(PowerUpdateEvent event) {}

}

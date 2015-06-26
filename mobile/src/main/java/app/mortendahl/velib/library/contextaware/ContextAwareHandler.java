package app.mortendahl.velib.library.contextaware;

import android.location.Location;

import com.google.android.gms.location.DetectedActivity;

public interface ContextAwareHandler {

    void onActivityUpdate(DetectedActivity detectedActivity);

    void onLocationUpdate(Location location);

}

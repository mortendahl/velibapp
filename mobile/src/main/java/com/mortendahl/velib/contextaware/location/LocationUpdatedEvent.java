package com.mortendahl.velib.contextaware.location;


import android.location.Location;

public class LocationUpdatedEvent {

    public final Location location;

    public LocationUpdatedEvent() {
        this.location = null;
    }

    public LocationUpdatedEvent(Location location) {
        this.location = location;
    }

}

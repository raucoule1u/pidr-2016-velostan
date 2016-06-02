package eu.telecomnancy.pidr_2016_velostan;

import android.location.Location;

import java.util.Date;

/**
 * Created by Yoann on 17/05/2016.
 */
public class LocationModel {

    private Location location;
    private Date date;
    private String device;

    public LocationModel(String device, Location location, Date date) {
        this.device = device;
        this.location = location;
        this.date = date;
    }

    public double getLatitude() {
        return location.getLatitude();
    }

    public double getLongitude() {
        return location.getLongitude();
    }

    public double getSpeed() {
        return (location.getSpeed()*(3.6)); // Vitesse en km/h
    }

    public Date getDate() {
        return date;
    }

    public String getDevice() { return device; }

    @Override
    public String toString() {
        return "(device="+this.device+", lat="+this.getLatitude()+", lng="+this.getLongitude()
                +", speed="+this.getSpeed()+", date="+this.date+")";
    }
}

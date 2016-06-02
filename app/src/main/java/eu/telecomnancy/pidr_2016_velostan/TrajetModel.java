package eu.telecomnancy.pidr_2016_velostan;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yoann on 19/05/2016.
 */
public class TrajetModel {

    private int id_trajet;
    private LocationModel location_debut;
    private LocationModel location_fin;
    private List<Double> speedAverage;

    public TrajetModel(LocationModel location_debut) {
        this.location_debut = location_debut;
        this.speedAverage = new ArrayList<>();
    }

    public int getId_trajet() {
        return id_trajet;
    }

    public void setId_trajet(int id_trajet) {
        this.id_trajet = id_trajet;
    }

    public LocationModel getLocation_debut() {
        return location_debut;
    }

    public LocationModel getLocation_fin() {
        return location_fin;
    }

    public void setLocation_fin(LocationModel location_fin) {
        this.location_fin = location_fin;
    }

    public void addSpeeds(List<LocationModel> locations) {
        double speedAvg = 0.0;
        for (LocationModel l : locations) {
            speedAvg += l.getSpeed();
        }
        speedAvg = speedAvg / (double) locations.size();
        this.speedAverage.add(speedAvg);
    }

    public double getSpeedAverage() {
        double speedAvg = 0.0;
        for (Double speed : this.speedAverage) {
            speedAvg += speed;
        }
        speedAvg = speedAvg / (double) this.speedAverage.size();

        Log.d("SPEED AVERAGE", this.speedAverage+"");
        return speedAvg;
    }
}

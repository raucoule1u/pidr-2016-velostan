package eu.telecomnancy.pidr_2016_velostan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

/**
 * Created by Yoann on 15/05/2016.
 */
public class GPSUpdateReceiver extends BroadcastReceiver {

    private final static List<LocationModel> locations = new ArrayList<>();
    private static TrajetModel trajet = null;
    private static LocationModel lastLocation = null;
    private static String device;
    private static long tempsDebut;
    private static Timer timer = null;

    JSONParser jsonParser = new JSONParser();
    private final static String formatDate = "dd/MM/yyyy HH:mm:ss";
    private final static String url_create_location = "http://webservice-velostan.rhcloud.com/webservice/create_location.php";
    private final static String url_create_trajet = "http://webservice-velostan.rhcloud.com/webservice/create_trajet.php";
    private final static String url_update_trajet = "http://webservice-velostan.rhcloud.com/webservice/update_trajet.php";
    private final static String url_last_location = "http://webservice-velostan.rhcloud.com/webservice/last_location.php";
    private final static String TAG_SUCCESS = "success";
    private final static int inactivityTime = 120; // in seconds
    private final static int checkTimer = 1000; // in milliseconds
    private final static int minLocationBeforeSending = 10; // in locations

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Location","onReceive called ("+locations+")");
        final Location location = (Location) intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED);

        if(location != null) {
            if (timer != null)
                timer.cancel();
            tempsDebut = System.currentTimeMillis();
            timer = new Timer();

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    long currentTime = System.currentTimeMillis();
                    float seconds = (currentTime - tempsDebut) / 1000F;
                    Log.d("TIMER", seconds+" secondes écoulées");
                    if (seconds > inactivityTime) { // inactivity during 5 minutes
                        Log.d("TRAJET", "Fin trajet");
                        // Stop the timer
                        timer.cancel();
                        // Get the last location
                        if ( !locations.isEmpty()) {
                            lastLocation = locations.get(locations.size() - 1);
                        } else {
                            try {
                                (new LastLocation(trajet)).execute().get();
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                        trajet.setLocation_fin(lastLocation);
                        trajet.addSpeeds(locations);

                        (new UpdateTrajet(trajet)).execute();

                        if ( !locations.isEmpty()) {
                            // Add the last locations in the database
                            for (LocationModel loc : locations) {
                                try {
                                    (new CreateNewLocation(loc)).execute().get();
                                } catch (InterruptedException | ExecutionException e) {
                                    e.printStackTrace();
                                }
                            }
                            locations.clear();
                        }

                        // Reset trajet
                        trajet = null;
                    }
                }
            }, 0, checkTimer);

            Date date = new Date();
            LocationModel locationModel = new LocationModel(device, location, date);
            addLocation(locationModel);
        } else {
            Log.d("Location","Location is null ");
        }
    }

    public static void setDevice(String device) {
        GPSUpdateReceiver.device = device;
    }

    private void addLocation(LocationModel l) {

        Log.d("Location","Location has been added ("+l+")");
        locations.add(l);

        String listLocations = "List:\n";
        for (LocationModel location : locations) {
            listLocations += location+"\n";
        }
        Log.d("Location",listLocations);

        // Nouveau trajet
        if (trajet == null) {
            Log.d("TRAJET", "Nouveau trajet");
            trajet = new TrajetModel(l);

            CreateNewTrajet trajetCreation = new CreateNewTrajet(trajet);

            String res = null;
            try {
                 res = trajetCreation.execute().get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            trajet.setId_trajet(trajetCreation.getTrajet().getId_trajet());
            Log.d("TRAJET","id_trajet = "+trajet.getId_trajet());
        }

        if (locations.size() == minLocationBeforeSending) {
            for (LocationModel loc : locations) {
                (new CreateNewLocation(loc)).execute();
            }
            trajet.addSpeeds(locations);
            locations.clear();
        }
    }

    /**
     * Background Async Task to Create new product
     * */
    class CreateNewLocation extends AsyncTask<String, String, String> {

        private LocationModel location;

        public CreateNewLocation(LocationModel location) {
            this.location = location;
        }

        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * Creating product
         * */
        protected String doInBackground(String... args) {
            DateFormat formatter = new SimpleDateFormat(formatDate);
            String dateString = formatter.format(location.getDate());

            // Building Parameters
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("device", location.getDevice()));
            params.add(new BasicNameValuePair("latitude", Double.toString(location.getLatitude())));
            params.add(new BasicNameValuePair("longitude", Double.toString(location.getLongitude())));
            params.add(new BasicNameValuePair("speed", Double.toString(location.getSpeed())));
            params.add(new BasicNameValuePair("date", dateString));
            params.add(new BasicNameValuePair("id_trajet", Integer.toString(trajet.getId_trajet())));

            // getting JSON Object
            // Note that create product url accepts POST method
            //jsonParser.makeHttpRequest(url_create_location, "POST", params);
            JSONObject json = jsonParser.makeHttpRequest(url_create_location,
                    "POST", params);

            // check log cat fro response
            Log.d("CREATE_LOCATION", "Create Response: "+json.toString());

            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);

                if (success == 1) {
                    // successfully created product
                    Log.d("CREATE_LOCATION","Success");
                } else {
                    // failed to create product
                    Log.d("CREATE_LOCATION","Fail");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        protected void onPostExecute(String file_url) {}

    }

    /**
     * Background Async Task to Create new product
     * */
    class CreateNewTrajet extends AsyncTask<String, String, String> {

        private TrajetModel trajet;

        public CreateNewTrajet(TrajetModel trajet) {
            this.trajet = trajet;
        }

        public TrajetModel getTrajet() {
            return this.trajet;
        }

        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * Creating product
         * */
        protected String doInBackground(String... args) {
            DateFormat formatter = new SimpleDateFormat(formatDate);
            String dateString = formatter.format(trajet.getLocation_debut().getDate());

            // Building Parameters
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("device", trajet.getLocation_debut().getDevice()));
            params.add(new BasicNameValuePair("longitude_debut", Double.toString(trajet.getLocation_debut().getLatitude())));
            params.add(new BasicNameValuePair("latitude_debut", Double.toString(trajet.getLocation_debut().getLongitude())));
            params.add(new BasicNameValuePair("date_debut", dateString));

            // getting JSON Object
            // Note that create product url accepts POST method
            //jsonParser.makeHttpRequest(url_create_trajet, "POST", params);
            JSONObject json = jsonParser.makeHttpRequest(url_create_trajet, "POST", params);

            // check log cat fro response
            Log.d("CREATE_TRAJET", "Create Response: "+json.toString());

            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);
                int id_trajet = json.getInt("id_trajet");
                trajet.setId_trajet(id_trajet);

                if (success == 1) {
                    // successfully created product
                    Log.d("CREATE_TRAJET","Success");
                } else {
                    // failed to create product
                    Log.d("CREATE_TRAJET","Fail");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        protected void onPostExecute(String file_url) {}

    }

    /**
     * Background Async Task to Create new product
     * */
    class UpdateTrajet extends AsyncTask<String, String, String> {

        private TrajetModel trajet;

        public UpdateTrajet(TrajetModel trajet) {
            this.trajet = trajet;
        }

        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * Creating product
         * */
        protected String doInBackground(String... args) {
            DateFormat formatter = new SimpleDateFormat(formatDate);
            String dateString = formatter.format(trajet.getLocation_fin().getDate());

            // Building Parameters
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("id_trajet", Integer.toString(trajet.getId_trajet())));
            params.add(new BasicNameValuePair("longitude_fin", Double.toString(trajet.getLocation_fin().getLatitude())));
            params.add(new BasicNameValuePair("latitude_fin", Double.toString(trajet.getLocation_fin().getLongitude())));
            params.add(new BasicNameValuePair("date_fin", dateString));
            params.add(new BasicNameValuePair("vitesse_moyenne", Double.toString(trajet.getSpeedAverage())));

            // getting JSON Object
            // Note that create product url accepts POST method
            //jsonParser.makeHttpRequest(url_update_trajet, "POST", params);
            JSONObject json = jsonParser.makeHttpRequest(url_update_trajet, "POST", params);

            // check log cat fro response
            Log.d("UPDATE_TRAJET", json.toString());

            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);

                if (success == 1) {
                    // successfully created product
                    Log.d("UPDATE_TRAJET","Success");
                } else {
                    // failed to create product
                    Log.d("UPDATE_TRAJET","Fail");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        protected void onPostExecute(String file_url) {}

    }

    /**
     * Background Async Task to Create new product
     * */
    class LastLocation extends AsyncTask<String, String, String> {

        private TrajetModel trajet;

        public LastLocation(TrajetModel trajet) {
            this.trajet = trajet;
        }

        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * Creating product
         * */
        protected String doInBackground(String... args) {
            // Building Parameters
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("id_trajet", Integer.toString(trajet.getId_trajet())));

            // getting JSON Object
            // Note that create product url accepts POST method
            JSONObject json = jsonParser.makeHttpRequest(url_last_location, "GET", params);

            // check log cat fro response
            Log.d("LAST_LOCATION", json.toString());

            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);
                double latitude = json.getDouble("latitude");
                double longitude = json.getDouble("longitude");
                float speed = (float) json.getDouble("speed");
                String date_location = json.getString("date_location");

                DateFormat formatter = new SimpleDateFormat(formatDate);
                Date date = null;
                try {
                    date = formatter.parse(date_location);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                // id_location device latitude longitude speed date_location id_trajet

                Location l = new Location(LocationManager.GPS_PROVIDER);
                l.setLatitude(latitude);
                l.setLongitude(longitude);
                l.setSpeed(speed);
                lastLocation = new LocationModel(device, l, date);

                if (success == 1) {
                    // successfully created product
                    Log.d("LAST_LOCATION","Success");
                } else {
                    // failed to create product
                    Log.d("LAST_LOCATION","Fail");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        protected void onPostExecute(String file_url) {}

    }

}

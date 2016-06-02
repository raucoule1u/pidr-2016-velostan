package eu.telecomnancy.pidr_2016_velostan;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Yoann on 15/05/2016.
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_LOCATION = 0;
    private static final int PERMISSION_REQUEST_DEVICE = 1;

    private final static long minTime = 1000; // 1 secondes
    private final static float minDistance = 20; // 20 mètres

    private CoordinatorLayout mainLayout;
    private TextView startText;

    private LocationManager locationManager;
    private PendingIntent pending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("PIDR", "Application running...");
        getDevice();

        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mainLayout = (CoordinatorLayout) findViewById(R.id.mainLayout);
        startText = (TextView) findViewById(R.id.startText);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        startLocation();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (pending != null) {
            locationManager.removeUpdates(pending);
            pending = null;
            Log.d("DESTROY","Pending has been removed and GPS is stopped");
        }
    }

    private void getDevice() {
        // Check if the Device permission has been denied
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already available, get device infos
            TelephonyManager mngr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            GPSUpdateReceiver.setDevice(mngr.getDeviceId());
        } else {
            // Permission is missing and must be requested.
            requestDevicePermission();
        }
    }

    private void startLocation() {
        // Check if the Location permission has been denied
        Log.d("LOCATION", "locations should be started");
// /*
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
// */
            // Permission is already available, start location
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            String provider = getBestProvider(locationManager);
            Log.d("PROVIDER", provider);
            if (!provider.equals(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(this, "Please turn on the GPS", Toast.LENGTH_LONG).show();
                startText.setText(R.string.errorGPS);
            }

            Intent intent = new Intent(this, GPSUpdateReceiver.class);
            pending = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            locationManager.requestLocationUpdates(provider, minTime, minDistance, pending);
// /*
        } else {
            // Permission is missing and must be requested.
            requestLocationPermission();
        }
// */
    }

    protected String getBestProvider(LocationManager locationManager) {

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(true);
        criteria.setCostAllowed(false);

        String providerName = locationManager.getBestProvider(criteria, true);

        Log.d("LOCATION_PROVIDER", "Provider = " + providerName);

        return providerName;
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            Snackbar.make(mainLayout, "Location access is required to use the application.",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_REQUEST_LOCATION);
                }
            }).show();
        } else {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        }
    }

    private void requestDevicePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_PHONE_STATE)) {

            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            Snackbar.make(mainLayout, "Device IMEI access is required to use the application.",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_PHONE_STATE},
                            PERMISSION_REQUEST_DEVICE);
                }
            }).show();
        } else {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    PERMISSION_REQUEST_DEVICE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Snackbar.make(mainLayout, "Location permission was granted.",
                            Snackbar.LENGTH_SHORT)
                            .show();
                    startLocation();
                } else {
                    // Permission request was denied.
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Snackbar.make(mainLayout, "Location permission was denied.",
                            Snackbar.LENGTH_SHORT)
                            .show();
                }
            }
            case PERMISSION_REQUEST_DEVICE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Snackbar.make(mainLayout, "Device permission was granted.",
                            Snackbar.LENGTH_SHORT)
                            .show();
                    getDevice();
                } else {
                    // Permission request was denied.
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Snackbar.make(mainLayout, "Device permission was denied.",
                            Snackbar.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

}
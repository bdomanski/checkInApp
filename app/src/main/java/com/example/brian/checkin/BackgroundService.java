package com.example.brian.checkin;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * Created by Brian on 12/1/2017.
 *
 * A background service to handle when
 * to remind a user to check in, even
 * while they are not using the app
 */

public class BackgroundService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private ServiceHandler mServiceHandler;

    // Helper to send notifications to user
    private NotificationHelper nh;

    private GoogleApiClient mGoogleApiClient;
    private Location lastLocation;

    private LocationService places;

    private Context context;

    private Boolean paused = false;
    private Boolean stopped = false;

    private int consecutiveRestaurants;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {

            int minutesToWait = 5;

            ConnectivityManager cm =
                    (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            // Only attempt to connect to Google API if connected to internet
            boolean isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();

            if(mGoogleApiClient.isConnected()) {
                Log.d("Last Location", places.getLastLocation().toString());

                if(consecutiveRestaurants == 0) {
                    lastLocation = places.getLastLocation();
                }
                boolean isRestaurant = places.isCurrentPlaceRestaurant();
                System.out.println("Is restaurant?: " + isRestaurant);

                if(consecutiveRestaurants == 3) {
                    nh.sendNotification();
                    consecutiveRestaurants = 0;
                }

                // Check that user is at a restaurant and is not moving quickly
                if(isRestaurant && places.getLastLocation().getSpeed() < 8) {

                    // Check user has not moved more than 50 meters
                    if(lastLocation != null && lastLocation.distanceTo(places.getLastLocation()) < 50) {
                        ++consecutiveRestaurants;
                    } else {
                        consecutiveRestaurants = 0;
                    }
                    System.out.println("Consecutive: " + consecutiveRestaurants);

                // else, reset the count
                } else {
                    System.out.println("Current speed: " + places.getLastLocation().getSpeed());
                    consecutiveRestaurants = 0;
                }
            } else if(isConnected){
                paused = true;
                mGoogleApiClient.connect();
                return;
            }

            // Timer before running again
            new CountDownTimer(minutesToWait * 60 * 1000, 2500) {

                public void onTick(long millisUntilFinished) {}

                public void onFinish() {
                    if(!stopped) startService(new Intent(context, BackgroundService.class));
                }

            }.start();
        }
    }

    @Override
    public void onCreate() {
        nh = new NotificationHelper(this);
        context = this;

        // Construct a GeoDataClient.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        HandlerThread thread = new HandlerThread("ServiceStartArguments", THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceHandler = new ServiceHandler(thread.getLooper());
        consecutiveRestaurants = 0;
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        //Toast.makeText(this, "Background Service", Toast.LENGTH_LONG).show();

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        // Runs until explicitly stopped
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopped = true;
        stopSelf();
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        System.err.println(result.toString());
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onLocationChanged(Location location) {}

    @Override
    public void onConnected(Bundle bundle) {
        // Create LocationService
        places = new LocationService(mGoogleApiClient, this, null);
        places.requestLocationUpdates();

        // Check that Google Maps API was told to connect because
        // it was needed for the restaurant check
        if(paused && !stopped) {
            startService(new Intent(context, BackgroundService.class));
            paused = false;
        }
    }
}

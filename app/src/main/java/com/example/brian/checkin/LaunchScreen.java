package com.example.brian.checkin;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import java.util.ArrayList;

public class LaunchScreen extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference userRef = database.getReference("userInput/");
    private final DatabaseReference placesRef = database.getReference("placesAPI/");

    // Used for selecting the current place.
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch_screen);

        // Construct a GeoDataClient.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10_000);
        locationRequest.setFastestInterval(5_000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void onQueryClick(View v) {
        EditText text_box = findViewById(R.id.location_input);

        userRef.push().setValue(text_box.getText().toString());

        getCurrentPlaces();
    }

    public void getCurrentPlaces() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            System.out.println("Permission Granted\n");
            mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLastKnownLocation != null) System.out.println(mLastKnownLocation.toString());
        } else {
            System.out.println("Permission Not Granted\n");
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 123);
        }

        PendingResult<PlaceLikelihoodBuffer> placeResult = Places.PlaceDetectionApi
                .getCurrentPlace(mGoogleApiClient, null);

        placeResult.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {

                // Debug info printed to console
                Status status = likelyPlaces.getStatus();
                System.out.println(status.isSuccess());
                System.out.println(status.getStatusCode());
                System.out.println(status.getStatusMessage());
                System.out.println(status.getStatus());

                TextView output = findViewById(R.id.output);
                String name;
                float likelihood;

                // Print depending on number of places
                output.setText(likelyPlaces.getCount() > 0 ? "Nearby Places:\n" : "No Nearby Places\n");

                // Print out
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    name = placeLikelihood.getPlace().getName().toString();
                    likelihood = placeLikelihood.getLikelihood();

                    if(likelihood > 0) {
                        output.append(name + ": " + likelihood);
                        placesRef.child("Name").push().setValue(name);
                        placesRef.child("Likelihood").push().setValue(likelihood);
                    }

                    // Debug info
                    System.out.println(placeLikelihood.getPlace().getName().toString());
                    System.out.println(placeLikelihood.getLikelihood());
                }
                likelyPlaces.release();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                getCurrentPlaces();
            } else {
                // User refused to grant permission. You can add AlertDialog here
                Toast.makeText(this, "You didn't give permission to access device location", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void requestLocationUpdates() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            System.out.println("Permission Granted\n");
            mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLastKnownLocation != null) System.out.println(mLastKnownLocation.toString());
        } else {
            System.out.println("Permission Not Granted\n");
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 123);
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,  locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastKnownLocation = location;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        database.goOnline();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        database.goOffline();
        super.onStop();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {}

    @Override
    public void onConnectionSuspended(int i) {}


    @Override
    public void onConnected(Bundle bundle) {
        requestLocationUpdates();
    }
}

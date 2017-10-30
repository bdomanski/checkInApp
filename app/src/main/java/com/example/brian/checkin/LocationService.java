package com.example.brian.checkin;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.firebase.database.DatabaseReference;

import java.util.List;

/**
 * Created by Brian on 10/27/2017.
 *
 */

public class LocationService extends FragmentActivity implements LocationListener {

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Output results to screen
    private TextView output;

    // Access to google api
    public GoogleApiClient mGoogleApiClient;

    // Firebase references
    private DatabaseReference pushRef;
    public DatabaseReference placesRef;

    // Used for selecting the current place.
    private LocationRequest locationRequest;

    // Used to access main activity's context
    private Context context;

    // Restaurant Filter
    private PlaceTypeFilter restaurantFilter =
            new PlaceTypeFilter(new int[]{Place.TYPE_RESTAURANT, Place.TYPE_FOOD}, new int[]{});

    private List<PlaceLikelihood> filterResult;

    LocationService(GoogleApiClient g, Context c, TextView t) {
        mGoogleApiClient = g;
        context = c;
        output = t;

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10_000);
        locationRequest.setFastestInterval(5_000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void getCurrentPlaces() {
        if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            System.out.println("Permission Granted\n");
            mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLastKnownLocation != null) System.out.println(mLastKnownLocation.toString());
        } else {
            System.out.println("Permission Not Granted\n");
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, 123);
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

                if(!status.isSuccess()) {
                    placesRef.child("placesAPI").setValue("ERROR");
                }

                filterResult = restaurantFilter.filteredPlaces(likelyPlaces);

                String name;
                float likelihood;

                // Print depending on number of places
                output.setText(likelyPlaces.getCount() > 0 ? "Nearby Places:\n" : "No Nearby Places\n");

                if(filterResult != null) {
                    // Print out
                    for (PlaceLikelihood placeLikelihood : filterResult) {
                        name = placeLikelihood.getPlace().getName().toString();
                        likelihood = placeLikelihood.getLikelihood();

                        if(likelihood > 0) {
                            output.append(name + ": " + likelihood + '\n');

                            pushRef = placesRef.child("placesAPI").push();
                            pushRef.child("Name").setValue(name);
                            pushRef.child("Likelihood").setValue(likelihood);
                        }

                        // Debug info
                        System.out.println(placeLikelihood.getPlace().getName().toString());
                        System.out.println(placeLikelihood.getLikelihood());
                    }
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

    public void requestLocationUpdates() {
        if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            System.out.println("Permission Granted\n");
            mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLastKnownLocation != null) System.out.println(mLastKnownLocation.toString());
        } else {
            System.out.println("Permission Not Granted\n");
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, 123);
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
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }
}

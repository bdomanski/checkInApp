package com.example.brian.checkin;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
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
import com.intentfilter.androidpermissions.PermissionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brian on 10/27/2017.
 *
 */

public class LocationService extends Service implements LocationListener {

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

    // Restaurant Filter
    private PlaceTypeFilter restaurantFilter =
            new PlaceTypeFilter(new int[]{Place.TYPE_RESTAURANT, Place.TYPE_FOOD}, new int[]{});

    private List<PlaceLikelihood> filterResult;

    // Check permissions
    private PermissionManager permissionManager;
    ArrayList<String> permissions = new ArrayList<>();

    private Context context;

    LocationService(GoogleApiClient g, Context c, TextView t) {
        mGoogleApiClient = g;
        output = t;
        context = c;

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10_000);
        locationRequest.setFastestInterval(5_000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public void getCurrentPlaces() {
        permissionManager = PermissionManager.getInstance(context);
        permissionManager.checkPermissions(permissions, new PermissionManager.PermissionRequestListener() {
            @Override
            public void onPermissionGranted() {
                //Toast.makeText(context,"Permission Granted",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied() {
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        });
        if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            System.out.println("Permission Granted\n");
            mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLastKnownLocation != null) System.out.println(mLastKnownLocation.toString());
        } else {
            System.out.println("Permission Not Granted\n");
            return;
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

                // Remove places with 0 chance
                if(filterResult != null) {
                    for(int i = 0; i < filterResult.size(); ++i) {
                        if(filterResult.get(i).getLikelihood() == 0) {
                            filterResult.remove(i);
                            --i;
                        }
                    }
                }

                String name;
                float likelihood;

                // Print depending on number of places
                output.setText(filterResult.size() > 0 ? "Nearby Places:\n\n" : "No Nearby Places\n\n");

                if(filterResult != null) {
                    // Print out
                    for (PlaceLikelihood placeLikelihood : filterResult) {
                        name = placeLikelihood.getPlace().getName().toString();
                        likelihood = placeLikelihood.getLikelihood();

                        if(likelihood > 0) {
                            output.append(name + ": " + likelihood + "\n\n");

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

    public void requestLocationUpdates() {
        permissionManager = PermissionManager.getInstance(context);
        permissionManager.checkPermissions(permissions, new PermissionManager.PermissionRequestListener() {
            @Override
            public void onPermissionGranted() {
                //Toast.makeText(getBaseContext(),"Permission Granted",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied() {
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        });
        if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            System.out.println("Permission Granted\n");
            mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLastKnownLocation != null) System.out.println(mLastKnownLocation.toString());
        } else {
            System.out.println("Permission Not Granted\n");
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,  locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastKnownLocation = location;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

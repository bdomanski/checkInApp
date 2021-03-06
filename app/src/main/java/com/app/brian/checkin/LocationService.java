package com.app.brian.checkin;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.NonNull;
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
import java.util.Date;
import java.util.concurrent.CountDownLatch;

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

    // To get unix timestamp
    private Date date;

    private Boolean found;

    LocationService(GoogleApiClient g, Context c, TextView t) {
        mGoogleApiClient = g;
        output = t;
        context = c;

        locationRequest = new LocationRequest();
        locationRequest.setInterval(5 * 60 * 1000);
        locationRequest.setFastestInterval(60 * 1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public void getCurrentPlaces(final DatabaseReference placesRef) {
        permissionManager = PermissionManager.getInstance(context);
        permissionManager.checkPermissions(permissions, new PermissionManager.PermissionRequestListener() {
            @Override
            public void onPermissionGranted() {
                //Toast.makeText(context,"Permission Granted",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied() {
                placesRef.child("placesAPI").setValue("Permission denied");
            }
        });
        if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            System.out.println("Permission Granted\n");
            mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLastKnownLocation != null) System.out.println(mLastKnownLocation.toString());
        } else {
            System.out.println("Permission Not Granted\n");
            placesRef.child("placesAPI").setValue("Permission not granted");
            return;
        }

        placesRef.child("placesAPI").setValue("Failed for unknown reason");
        final CountDownLatch latch = new CountDownLatch(1);

        PendingResult<PlaceLikelihoodBuffer> placeResult = Places.PlaceDetectionApi
                .getCurrentPlace(mGoogleApiClient, null);

        placeResult.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(@NonNull PlaceLikelihoodBuffer likelyPlaces) {

                // Debug info printed to console
                Status status = likelyPlaces.getStatus();
                System.out.println(status.getStatusMessage());
                System.out.println(status.getStatus());

                if(!status.isSuccess()) {
                    placesRef.child("placesAPI").setValue("ERROR");
                }

                filterResult = restaurantFilter.filteredPlaces(likelyPlaces);

                String name;
                float likelihood;

                if(filterResult != null) {
                    if(filterResult.size() == 0) {
                        placesRef.child("PlacesAPI").setValue("No Nearby Places");
                    }
                    date = new Date(); // get current time
                    placesRef.child("Time").setValue(date.getTime());

                    int i = 0;

                    // Print out
                    for (PlaceLikelihood placeLikelihood : filterResult) {
                        name = placeLikelihood.getPlace().getName().toString();
                        likelihood = placeLikelihood.getLikelihood();


                        if(likelihood > 0) {
                            DatabaseReference pushRef = placesRef.child("placesAPI").child(String.valueOf(i++));
                            pushRef.child("Name").setValue(name);
                            pushRef.child("Likelihood").setValue(likelihood);
                            pushRef.child("Latitude").setValue(placeLikelihood.getPlace().getLatLng().latitude);
                            pushRef.child("Longitude").setValue(placeLikelihood.getPlace().getLatLng().longitude);
                        }

                        // Debug info
                        System.out.println(placeLikelihood.getPlace().getName().toString());
                        System.out.println(placeLikelihood.getLikelihood());
                    }
                }
                latch.countDown();

                output.setText("Data successfully sent!\n\n");

                // Print depending on number of places
                output.append((filterResult != null) && filterResult.size() > 0 ?
                        "Nearby restaurants found" : "No nearby restaurants");

                likelyPlaces.release();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            System.err.println("Latch interrupted");
        }
    }

    public Boolean isCurrentPlaceRestaurant() {
        permissionManager = PermissionManager.getInstance(context);
        permissionManager.checkPermissions(permissions, new PermissionManager.PermissionRequestListener() {
            @Override
            public void onPermissionGranted() {
                //Toast.makeText(context,"Permission Granted",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied() {
                //Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        });
        if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            System.out.println("Permission Granted\n");
            mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLastKnownLocation != null) System.out.println(mLastKnownLocation.toString());
        } else {
            System.out.println("Permission Not Granted\n");
            return false;
        }

        found = false;

        PendingResult<PlaceLikelihoodBuffer> placeResult = Places.PlaceDetectionApi
                .getCurrentPlace(mGoogleApiClient, null);

        final CountDownLatch latch = new CountDownLatch(1);

        placeResult.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(@NonNull PlaceLikelihoodBuffer likelyPlaces) {

                // Debug info printed to console
                Status status = likelyPlaces.getStatus();
                System.out.println(status.getStatus());

                filterResult = restaurantFilter.filteredPlaces(likelyPlaces);

                if(filterResult != null) {
                    Location loc = new Location("current");

                    if(likelyPlaces.getCount() > 0 && filterResult.size() > 0) {
                        PlaceLikelihood curr_place = filterResult.get(0);
                        loc.setLatitude(curr_place.getPlace().getLatLng().latitude);
                        loc.setLongitude(curr_place.getPlace().getLatLng().longitude);
                        if(likelyPlaces.get(0).getPlace().getName().equals(curr_place.getPlace().getName()) ||
                           likelyPlaces.get(1).getPlace().getName().equals(curr_place.getPlace().getName()) ||
                           mLastKnownLocation.distanceTo(loc) < 25) {

                            found = true;
                        }
                    }
                    latch.countDown();
                }
                likelyPlaces.release();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            System.err.println("Latch interrupted");
        }

        return found;
    }

    public Location getLastLocation() {
        return mLastKnownLocation;
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

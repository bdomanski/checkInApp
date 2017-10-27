package com.example.brian.checkin;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class LaunchScreen extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    // Code that should be received when requesting account
    private static final int REQUEST_CODE_EMAIL = 1;

    private EditText text_box;

    private GoogleApiClient mGoogleApiClient;

    // Reference to Firebase Database
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef;
    private DatabaseReference pushRef;

    // Calls places API
    private LocationService places;

    // User ID to push to Firebase
    private String userID = "null";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch_screen);
        text_box = findViewById(R.id.location_input);

        // Construct a GeoDataClient.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        places = new LocationService();
        places.setContext(this);

        mGoogleApiClient.connect();

        //requestAccount();
    }

    public void onQueryClick(View v) {
        userRef = database.getReference(userID);
        pushRef = userRef.push();
        pushRef.child("userInput").setValue(text_box.getText().toString());

        places.placesRef = pushRef;
        places.requestLocationUpdates();
        places.getCurrentPlaces();
    }

    public void requestAccount() {

        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
                false, null, null, null, null);

        try {
            startActivityForResult(intent, REQUEST_CODE_EMAIL);
        } catch (ActivityNotFoundException e) {
            userRef = database.getReference(userID);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EMAIL && resultCode == RESULT_OK) {
            userID = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            return;
        }

        // Set location to push data
        userRef = database.getReference(userID);

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onLocationChanged(Location location) {}

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        System.err.println(result.toString());
    }

    @Override
    public void onConnectionSuspended(int i) {}


    @Override
    public void onConnected(Bundle bundle) {
        System.out.println("Time to request location updates");
        places.mGoogleApiClient = mGoogleApiClient;
        places.requestLocationUpdates();
        System.out.println("Done");
    }
}

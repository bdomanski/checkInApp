package com.example.brian.checkin;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class LaunchScreen extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private EditText text_box;
    private TextView out;
    private TextView keyOut;

    private GoogleApiClient mGoogleApiClient;

    // Reference to Firebase Database
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef;

    // Calls places API
    private LocationService places;

    // User ID to push to Firebase
    private String userID = "null";

    // Get and set userID that stays constant while app is installed
    private PreferencesHelper ph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch_screen);
        text_box = findViewById(R.id.location_input);

        out = findViewById(R.id.output);
        keyOut = findViewById(R.id.keyOut);

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

        ph = new PreferencesHelper(this);
        setPreferences();
        if(keyOut != null) keyOut.setText(userID);
    }

    public void onQueryClick(View v) {
        userRef = database.getReference(userID);
        DatabaseReference pushRef = userRef.push();
        pushRef.child("userInput").setValue(text_box.getText().toString());

        places.placesRef = pushRef;

        if(mGoogleApiClient.isConnected()) {
            places.requestLocationUpdates();
            places.getCurrentPlaces();
        } else {
            mGoogleApiClient.connect();
        }
    }

    public void onCopyClick(View v) {
        setClipboard(keyOut.getText().toString());
    }

    private void setPreferences() {
        if(ph.empty()) {

            // Set new userID if one does not exist
            userRef = database.getReference();
            userID = userRef.push().getKey();
            ph.setKey(userID);
        } else {

            // Get userID
            userID = ph.getKey();
        }
    }

    private void setClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
        if(clipboard != null) clipboard.setPrimaryClip(clip);
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
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        System.err.println(result.toString());
    }

    @Override
    public void onConnectionSuspended(int i) {}


    @Override
    public void onConnected(Bundle bundle) {
        places = new LocationService(mGoogleApiClient, this, out);
        places.requestLocationUpdates();
    }
}

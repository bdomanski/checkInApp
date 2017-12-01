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
import android.content.Intent;
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

    // Helper to send notifications to user
    private NotificationHelper nh;

    // Used to send user notifications when they are at a restaurant
    private BackgroundService backgroundService = new BackgroundService();
    private Intent bgIntent = new Intent(this, BackgroundService.class);

    private Boolean clicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch_screen);
        text_box = findViewById(R.id.location_input);

        out = findViewById(R.id.output);
        TextView keyOut = findViewById(R.id.keyOut);

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
        if(keyOut != null) keyOut.setText(userID.substring(userID.length() - 8));

        nh = new NotificationHelper(this);
    }

    public void onQueryClick(View v) {
        clicked = true; // Button was just clicked

        if(mGoogleApiClient.isConnected()) {
            database.goOnline();

            userRef = database.getReference(userID);
            DatabaseReference pushRef = userRef.child(String.valueOf(ph.getQueries()));

            // Add to the number of queries made
            ph.updateQueries();

            pushRef.child("UserInput").setValue(text_box.getText().toString());

            places.requestLocationUpdates();
            places.getCurrentPlaces(pushRef);
        } else {
            // onQueryClick() will be called again in onConnected()
            mGoogleApiClient.connect();
        }

        clicked = false; // Button done being clicked
    }

    public void onCopyClick(View v) {
        setClipboard(userID.substring(userID.length() - 8));
        nh.sendNotification();
    }

    private void setPreferences() {
        if(ph.empty()) {

            // Set new userID if one does not exist
            userRef = database.getReference();
            userID = userRef.push().getKey();
            ph.setKey(userID);

            ph.setQueries();
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
    protected void onPause() {
        backgroundService.startService(bgIntent);
        super.onPause();
    }

    @Override
    protected void onResume() {
        backgroundService.stopService(bgIntent);
        super.onResume();
        mGoogleApiClient.connect();
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

        // If called from onQueryClick, return to function
        if(clicked) onQueryClick(this.findViewById(R.id.btn_query));
    }
}

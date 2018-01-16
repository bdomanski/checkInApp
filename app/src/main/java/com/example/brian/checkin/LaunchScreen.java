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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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

    private Boolean clicked = false;
    private Boolean recentlyClicked = false;

    private int ratelimit;

    final Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);

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

        fadeOut.setDuration(3000);
        fadeOut.setStartOffset(10000);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                out.setText("");
            }
        });
    }

    public void onQueryClick(View v) {

        if(!isConnected()) {
            String string_out = "No internet connection";
            out.setText(string_out);
            out.startAnimation(fadeOut);
            return;
        }

        if(recentlyClicked) {
            limitRate();

        } else {
            justClicked();

            if(mGoogleApiClient.isConnected()) {
                database.goOnline();

                userRef = database.getReference(userID);
                DatabaseReference pushRef = userRef.child(String.valueOf(ph.getQueries()));

                // Add to the number of queries made
                ph.updateQueries();

                pushRef.child("UserInput").setValue(text_box.getText().toString());

                places.requestLocationUpdates();
                places.getCurrentPlaces(pushRef);
                out.startAnimation(fadeOut);
            } else {
                // onQueryClick() will be called again in onConnected()
                mGoogleApiClient.connect();
            }

            clicked = false; // Button done being clicked
        }
    }

    public void onCopyClick(View v) {
        setClipboard(userID.substring(userID.length() - 8));
    }

    private void justClicked() {
        int limit = 10; // Rate limit in minutes
        clicked = true; // Button was just clicked
        recentlyClicked = true;

        new CountDownTimer(limit * 60000, 1000) {

            public void onTick(long millisUntilFinished) {
                ratelimit = (int)millisUntilFinished / 1000;
            }

            public void onFinish() {
                recentlyClicked = false;
            }

        }.start();
    }

    private void limitRate() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.MINUTE, ratelimit / 60);
        c.add(Calendar.SECOND, ratelimit % 60);
        c.set(Calendar.AM_PM, Calendar.AM);

        String time = new SimpleDateFormat("HH:mm:ss").format(c.getTime());
        Toast.makeText(this, "Try again at " + time, Toast.LENGTH_LONG).show();

    }

    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = null;
        if(cm != null) activeNetwork = cm.getActiveNetworkInfo();

        // Only attempt to connect to Google API if connected to internet
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
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
        stopService(new Intent(this, BackgroundService.class));
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        startService(new Intent(this, BackgroundService.class));
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
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

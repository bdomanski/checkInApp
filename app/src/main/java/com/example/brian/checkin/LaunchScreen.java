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
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LaunchScreen extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private EditText text_box;
    private TextView out;

    private GoogleApiClient mGoogleApiClient;

    // Reference to Firebase Database
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef;

    // Calls places API
    private LocationService places;

    private Boolean clicked = false;

    // User ID to push to Firebase
    private String userID = "null";

    // Get and set userID that stays constant while app is installed
    private PreferencesHelper ph;

    // Animation for text to disappear
    final Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);

    // Construct a navigation list
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch_screen);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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

        // Set up app preferences helper
        ph = new PreferencesHelper(this);
        setPreferences();
        if(keyOut != null) keyOut.setText(userID.substring(userID.length() - 8));

        // Initialize navigation drawer
        mDrawerList = findViewById(R.id.navList);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        addDrawerItems();
        setupDrawer();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle("Home");
        }

        // Fade out animation for text
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
        Date date = new Date();
        int minutes = 10;

        if(!isConnected()) {
            String string_out = "No internet connection";
            out.setText(string_out);
            out.startAnimation(fadeOut);
            return;
        }

        if(date.getTime() < ph.getTime()) {
            limitRate();

        } else {
            justClicked(date.getTime());

            if(mGoogleApiClient.isConnected()) {
                database.goOnline();

                userRef = database.getReference(userID);
                DatabaseReference pushRef = userRef.child(String.valueOf(ph.getQueries()));

                DateFormat df = android.text.format.DateFormat.getMediumDateFormat(getApplicationContext());

                // Add user input and the number of queries made to app storage
                ph.addUserString(df.format(date) + ": " + text_box.getText().toString());
                ph.updateQueries();

                pushRef.child("UserInput").setValue(text_box.getText().toString());

                places.requestLocationUpdates();
                places.getCurrentPlaces(pushRef);
                out.startAnimation(fadeOut);
            } else {
                // onQueryClick() will be called again in onConnected()
                mGoogleApiClient.connect();
            }

            clicked = false;
        }
    }

    public void onCopyClick(View v) {
        setClipboard(userID.substring(userID.length() - 8));
    }

    private void justClicked(long time) {
        clicked = true;
        ph.setTime(time, 10);
    }

    private void limitRate() {
        String time = new SimpleDateFormat("HH:mm:ss").format(ph.getTime());
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

    private void addDrawerItems() {
        String[] navArray = { "Home", "History" };
        ArrayAdapter<String> mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, navArray);
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), HistoryActivity.class);

                if(id == 1) {
                    startActivity(intent);
                }
            }
        });
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /* Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Options");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /* Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Home");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
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

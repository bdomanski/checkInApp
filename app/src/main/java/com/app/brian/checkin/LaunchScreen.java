package com.app.brian.checkin;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.intentfilter.androidpermissions.PermissionManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class LaunchScreen extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private EditText text_box;
    private TextView out;

    private GoogleApiClient mGoogleApiClient;

    // References to Firebase Database and Storage
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef, pushRef;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef = storage.getReference();

    // Image to be taken with check in
    private String mediaFile = null, mediaFile2 = null;

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

    private static final int CAMERA_REQUEST = 1;
    private ImageView add_picture, add_picture2;
    private Bitmap photo, photo2;

    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch_screen);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Disable file URI exposure
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        PermissionManager permissionManager = PermissionManager.getInstance(this);
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
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

        add_picture = findViewById(R.id.imageView);
        add_picture2 = findViewById(R.id.imageView2);
        add_picture.setClickable(true);
        add_picture2.setClickable(true);

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
        Boolean fromNotification = false;

        if(!checkValidCheckIn()) return;

        if(date.getTime() < ph.getTime()) {
            limitRate();

        } else {

            if(mGoogleApiClient.isConnected()) {
                database.goOnline();

                userRef = database.getReference(userID);
                pushRef = userRef.child(String.valueOf(ph.getQueries()));

                DateFormat df = android.text.format.DateFormat.getMediumDateFormat(getApplicationContext());

                try {
                    uploadFile(0);
                    uploadFile(1);
                } catch(java.io.FileNotFoundException e) {
                    String string_out = "File not found";
                    out.setText(string_out);
                    out.startAnimation(fadeOut);
                    return;
                }

                // Reset file so user must take another
                mediaFile = null;
                mediaFile2 = null;

                places.requestLocationUpdates();
                places.getCurrentPlaces(pushRef);

                // Add user input and the number of queries made to app storage
                ph.addUserString(df.format(date) + ": " + text_box.getText().toString());
                ph.updateQueries();

                // Check if user checked in within 10 minutes of the notification
                if(date.getTime() < ph.getLastNotification() + 10 * 60 * 1000) fromNotification = true;

                pushRef.child("UserInput").setValue(text_box.getText().toString());
                pushRef.child("NumNotifications").setValue(ph.getNumNotifications());
                pushRef.child("From Notification?").setValue(fromNotification);
                justClicked(date.getTime());

                out.startAnimation(fadeOut);
            } else {
                // onQueryClick() will be called again in onConnected()
                mGoogleApiClient.connect();
            }

            clicked = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CAMERA_REQUEST) {
            photo = (Bitmap) data.getExtras().get("data");
            add_picture.setImageBitmap(photo);
        } else if (resultCode == RESULT_OK && requestCode == CAMERA_REQUEST + 1) {
            photo2 = (Bitmap) data.getExtras().get("data");
            add_picture2.setImageBitmap(photo2);
        }
    }

    public void selectImage(View v) {
        Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (camera_intent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            int id = v.getId();
            try {
                photoFile = createImageFile(id == R.id.imageView ? 0 : 1);
            } catch (IOException e) {
                System.out.println("Error creating file");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                startActivityForResult(camera_intent, CAMERA_REQUEST + (id == R.id.imageView ? 0 : 1));
            }
        }
    }

    private File createImageFile(int num) throws IOException {
        // Create an image file name
        String imageFileName = "check_in_" + Integer.toString(ph.getQueries()) + "." + Integer.toString(num);
        System.err.println(ph.getQueries());
        System.err.println(imageFileName);
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        System.err.println(image.toString());

        // Save a file: path for use with ACTION_VIEW intents
        if(num == 0) {
            mediaFile = image.getAbsolutePath();
        } else {
            mediaFile2 = image.getAbsolutePath();
        }
        return image;
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

    Boolean checkValidCheckIn() {

        if(!isConnected()) {
            String string_out = "No internet connection";
            out.setText(string_out);
            out.startAnimation(fadeOut);
            return false;
        }

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            String string_out = "App requires location permission";
            out.setText(string_out);
            out.startAnimation(fadeOut);
            return false;
        }

        // Empty string as input
        if(text_box.getText().toString().equals("")) {
            String string_out = "Please enter a location";
            out.setText(string_out);
            out.startAnimation(fadeOut);
            return false;
        }

        // User must include picture
        if(mediaFile == null || mediaFile2 == null) {
            String string_out = "Please include two pictures";
            out.setText(string_out);
            out.startAnimation(fadeOut);
            return false;
        }
        return true;
    }

    private void uploadFile(final int num) throws java.io.FileNotFoundException {
        String num_str = "." + Integer.toString(num);
        final StorageReference picRef = storageRef.child(userID + "/check_in_" + Integer.toString(ph.getQueries()) + num_str + ".jpg");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(num == 0) photo.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        else photo2.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        final UploadTask uploadTask = picRef.putBytes(data);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(getApplicationContext(), "Picture could not upload", Toast.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                /*picRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        pushRef.child("Image Location " + Integer.toString(num)).setValue(uri.toString());
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        pushRef.child("Image Location " + Integer.toString(num)).setValue("Failed to get image url");
                    }
                });*/
            }
        });
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnSuccessListener(this, new  OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {}
        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e("Firebase", "signInAnonymously:FAILURE", exception);
                    }
                });
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
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {}
        else {
            signInAnonymously();
        }
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

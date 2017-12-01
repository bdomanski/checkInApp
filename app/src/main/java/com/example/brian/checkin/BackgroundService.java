package com.example.brian.checkin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Brian on 12/1/2017.
 *
 * A background service to handle when
 * to remind a user to check in, even
 * while they are not using the app
 */

public class BackgroundService extends Service {

    @Override
    public void onCreate() {

    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {


        // Runs until explicitly stopped
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

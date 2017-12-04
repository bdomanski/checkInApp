package com.example.brian.checkin;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * Created by Brian on 12/1/2017.
 *
 * A background service to handle when
 * to remind a user to check in, even
 * while they are not using the app
 */

public class BackgroundService extends Service {

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    // Helper to send notifications to user
    private NotificationHelper nh;

    private Context context;

    private Boolean stopped = false;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(final Message msg) {

            //nh.sendNotification();

            new CountDownTimer(5000, 2500) {

                public void onTick(long millisUntilFinished) {}

                public void onFinish() {
                    if(!stopped) startService(new Intent(context, BackgroundService.class));
                }

            }.start();
        }
    }

    @Override
    public void onCreate() {
        nh = new NotificationHelper(this);
        context = this;

        HandlerThread thread = new HandlerThread("ServiceStartArguments", THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        Toast.makeText(this, "Background Service", Toast.LENGTH_LONG).show();

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        stopped = false;

        // Runs until explicitly stopped
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopped = true;
        stopSelf();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

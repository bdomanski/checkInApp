package com.example.brian.checkin;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import static android.app.Notification.DEFAULT_ALL;

/**
 * Created by Brian on 12/1/2017.
 * A helper class to handle notification
 * related functions
 */

public class NotificationHelper {

    private NotificationManager mNotificationManager;
    private Context context;

    public NotificationHelper(Context c) {
        context = c;

        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // The id of the channel.
        String id = "reminder";

        // The user-visible name of the channel.
        CharSequence name = context.getString(R.string.channel_name);

        // The user-visible description of the channel.
        /*String description = context.getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);

        // Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.enableLights(true);

        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.
        mChannel.enableVibration(true);
        mNotificationManager.createNotificationChannel(mChannel);*/
    }

    public void sendNotification() {

        // The id of the channel.
        String CHANNEL_ID = "reminder";
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                        .setContentTitle("Would you like to check in?")
                        .setDefaults(DEFAULT_ALL)
                        .setContentText("It appears you are at a restaurant");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, LaunchScreen.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your app to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(LaunchScreen.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // mNotificationId is a unique integer your app uses to identify the
        // notification. For example, to cancel the notification, you can pass its ID
        // number to NotificationManager.cancel().
        mNotificationManager.notify(0, mBuilder.build());
    }
}

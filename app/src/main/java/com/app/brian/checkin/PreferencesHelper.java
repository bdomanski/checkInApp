package com.app.brian.checkin;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brian on 11/6/2017.
 * Helper class for setting a userID preference
 */

class PreferencesHelper {
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    PreferencesHelper(Context context) {
        pref = context.getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor = pref.edit();
        editor.apply();
    }

    void setKey(String s) {
        editor.putString("userID", s);
        editor.commit();
    }

    void setQueries() {
        editor.putInt("Queries", 0);
        editor.commit();
    }

    void setTime(long time, int minutes) {
        // Add minutes in milliseconds
        editor.putLong("Time", time + minutes * 60 * 1000);
        editor.commit();
    }

    void setLastNotification(long time) {
        editor.putLong("Last Notification", time);
        editor.commit();
    }

    void addNumNotifications() {
        if(pref.contains("Num Notifications")) {
            editor.putInt("Num Notifications", 1);
        } else {
            editor.putInt("Num Notifications", getNumNotifications() + 1);
        }
    }

    void updateQueries() {
        editor.putInt("Queries", getQueries() + 1);
        editor.commit();
    }

    void addUserString(String s) {
        editor.putString(Integer.toString(getQueries()), s);
        editor.commit();
    }

    int getQueries() {
        return pref.getInt("Queries", 0);
    }

    String getKey() {
        return pref.getString("userID", "");
    }

    List<String> getUserStrings() {
        ArrayList<String> list = new ArrayList<>();
        for(int i = 0; i < getQueries(); ++i) {
            list.add(pref.getString(Integer.toString(i), "missing entry"));
        }
        return list;
    }

    long getTime() {
        return pref.getLong("Time", 0);
    }

    long getLastNotification() { return pref.getLong("Last Notification", 0); }

    int getNumNotifications() { return pref.getInt("Num Notifications", 0); }

    boolean empty() {
        return !pref.contains("userID");
    }
}

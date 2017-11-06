package com.example.brian.checkin;

import android.content.Context;
import android.content.SharedPreferences;

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

    String getKey() {
        return pref.getString("userID", "");
    }

    boolean empty() {
        return !pref.contains("userID");
    }
}

package com.example.brian.checkin;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.iid.InstanceID;

/**
 * Created by Brian on 11/30/2017.
 */

public class RegistrationService extends IntentService {
    public RegistrationService() {
        super("RegistrationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        InstanceID myID = InstanceID.getInstance(this);
    }

}

package com.kii.app.android.gcm;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by ryoichi.kawahara on 2015/12/12.
 */
public class GCMPreference {
    private static final String PREFERENCE_NAME = "GcmPreference";
    private static final String PROPERTY_REG_ID = "GCMregId";

    public static String getRegistrationId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        return registrationId;
    }

    public static void setRegistrationId(Context context, String regId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.commit();
    }
}

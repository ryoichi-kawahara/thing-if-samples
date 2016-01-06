package com.kii.app.android.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.kii.cloud.storage.DirectPushMessage;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.PushMessageBundleHelper;
import com.kii.cloud.storage.PushToAppMessage;
import com.kii.cloud.storage.PushToUserMessage;
import com.kii.cloud.storage.ReceivedMessage;

/**
 * Created by ryoichi.kawahara on 2015/12/12.
 */
public class KiiPushBroadcastReceiver extends BroadcastReceiver {

    public interface Observer {
        void onReceiveMessage(String returnedID);
    }

    private Observer mObserver;


    public KiiPushBroadcastReceiver() {
    }

    public KiiPushBroadcastReceiver(Observer observer) {
        mObserver = observer;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        debug("onReceive !!");
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        String messageType = gcm.getMessageType(intent);
        if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            debug("messageType = " + messageType);
            // Get the message as a bundle.
            Bundle extras = intent.getExtras();
            // Get the command ID from the payload.
            String returnedID = extras.getString("commandID");
            mObserver.onReceiveMessage(returnedID);
        }
    }

    static void debug(String message){
        Log.d("ThingIFSample", message);
    }
}


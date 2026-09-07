package com.rahimshop.admin;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "RahimShopFCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        Log.d(TAG, "New FCM token received");
        Log.d(TAG, "Token length: " + token.length());

        // MainActivity يقوم حاليًا بإرسال الـToken إلى السيرفر.
        // هنا نسجل فقط تغيّر الـToken.
    }
}

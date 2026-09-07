package com.rahimshop.admin;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "RahimShopFCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        Log.d(TAG, "FCM Token received");

        sendTokenToServer(token);
    }

    private void sendTokenToServer(String token) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(
                        "https://shop-dz.gt.tc/admin/save_push_token.php"
                );

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                String json =
                        "{"
                                + "\"token\":\""
                                + escapeJson(token)
                                + "\","
                                + "\"platform\":\"android\""
                                + "}";

                OutputStream outputStream =
                        connection.getOutputStream();

                outputStream.write(
                        json.getBytes("UTF-8")
                );

                outputStream.flush();
                outputStream.close();

                int responseCode =
                        connection.getResponseCode();

                Log.d(
                        TAG,
                        "Server response: " + responseCode
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Token upload failed",
                        e
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}

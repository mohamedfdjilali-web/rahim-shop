```java
package com.rahimshop.admin;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private WebView webView;

    private static final String TAG = "RahimShopFCM";

    private static final String TOKEN_URL =
            "https://shop-dz.gt.tc/admin/save_push_token.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        webView.setWebViewClient(new WebViewClient());

        setContentView(webView);

        webView.loadUrl(
                "https://shop-dz.gt.tc/admin/login.php"
        );

        getFCMToken();
    }

    private void getFCMToken() {

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        Log.e(
                                TAG,
                                "FCM Token failed",
                                task.getException()
                        );

                        return;
                    }

                    String token = task.getResult();

                    if (token == null || token.isEmpty()) {

                        Log.e(
                                TAG,
                                "FCM Token is empty"
                        );

                        return;
                    }

                    Log.d(
                            TAG,
                            "FCM TOKEN RECEIVED"
                    );

                    Log.d(
                            TAG,
                            "Token length: " + token.length()
                    );

                    sendTokenToServer(token);
                });
    }

    private void sendTokenToServer(String token) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(TOKEN_URL);

                connection =
                        (HttpURLConnection) url.openConnection();

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

                String escapedToken =
                        token
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"");

                String json =
                        "{"
                                + "\"token\":\""
                                + escapedToken
                                + "\","
                                + "\"platform\":\"android\""
                                + "}";

                Log.d(
                        TAG,
                        "Sending token to server..."
                );

                OutputStream output =
                        connection.getOutputStream();

                output.write(
                        json.getBytes("UTF-8")
                );

                output.flush();
                output.close();

                int responseCode =
                        connection.getResponseCode();

                Log.d(
                        TAG,
                        "HTTP response: " + responseCode
                );

                BufferedReader reader;

                if (responseCode >= 200 &&
                        responseCode < 400) {

                    reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            connection.getInputStream()
                                    )
                            );

                } else {

                    reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            connection.getErrorStream()
                                    )
                            );
                }

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {

                    response.append(line);
                }

                reader.close();

                Log.d(
                        TAG,
                        "Server response: "
                                + response
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

    @Override
    public void onBackPressed() {

        if (webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}
```

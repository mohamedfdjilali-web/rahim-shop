package com.rahimshop.admin;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.firebase.messaging.FirebaseMessaging;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    private WebView webView;

    private static final String TOKEN_URL =
            "https://shop-dz.gt.tc/admin/save_push_token_get.php";

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
                                "RahimShopFCM",
                                "FCM Token failed",
                                task.getException()
                        );

                        return;
                    }

                    String token = task.getResult();

                    Log.d(
                            "RahimShopFCM",
                            "FCM TOKEN length: " +
                                    token.length()
                    );

                    sendTokenToServer(token);
                });
    }

    private void sendTokenToServer(String token) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                String encodedToken =
                        URLEncoder.encode(
                                token,
                                "UTF-8"
                        );

                String urlString =
                        TOKEN_URL +
                        "?token=" +
                        encodedToken;

                URL url =
                        new URL(urlString);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");

                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                int responseCode =
                        connection.getResponseCode();

                Log.d(
                        "RahimShopFCM",
                        "Token upload HTTP: " +
                                responseCode
                );

                connection.disconnect();

            } catch (Exception e) {

                Log.e(
                        "RahimShopFCM",
                        "Token upload failed",
                        e
                );

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

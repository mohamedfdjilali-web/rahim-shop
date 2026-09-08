package com.rahimshop.admin;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    // =========================================================
    // SHOP-DZ WEBSITE
    // =========================================================

    private static final String WEBSITE_URL =
            "https://shop-dz.gt.tc/admin/login.php";


    // =========================================================
    // SUPABASE
    // =========================================================

    private static final String SUPABASE_URL =
            "https://tkntrbjsdxhizebascai.supabase.co";

    private static final String SUPABASE_PUBLISHABLE_KEY =
            "sb_publishable_P5ElVLCeoQrLdrbfI7G6Qg_hVR51Evo";


    // =========================================================
    // SUPABASE RPC
    // =========================================================

    private static final String SUPABASE_RPC_URL =
            SUPABASE_URL
                    + "/rest/v1/rpc/save_fcm_token";


    // =========================================================
    // WEBVIEW
    // =========================================================

    private WebView webView;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // فتح الموقع مباشرة
        setupWebView();

        // إذن الإشعارات Android 13+
        requestNotificationPermission();

        // الحصول على FCM Token وحفظه في Supabase
        getFCMToken();
    }


    // =========================================================
    // SETUP WEBVIEW
    // =========================================================

    private void setupWebView() {

        webView = new WebView(this);

        WebSettings settings =
                webView.getSettings();

        // JavaScript
        settings.setJavaScriptEnabled(true);

        // Local Storage
        settings.setDomStorageEnabled(true);

        // Database
        settings.setDatabaseEnabled(true);

        // Display
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // Zoom
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);

        // File access
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // -----------------------------------------------------
        // WebViewClient
        // -----------------------------------------------------

        webView.setWebViewClient(
                new WebViewClient()
        );

        // -----------------------------------------------------
        // WebChromeClient
        // -----------------------------------------------------

        webView.setWebChromeClient(
                new WebChromeClient()
        );

        // -----------------------------------------------------
        // وضع WebView كواجهة التطبيق
        // -----------------------------------------------------

        setContentView(webView);

        // -----------------------------------------------------
        // فتح SHOP-DZ
        // -----------------------------------------------------

        webView.loadUrl(
                WEBSITE_URL
        );
    }


    // =========================================================
    // NOTIFICATION PERMISSION
    // =========================================================

    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (
                    checkSelfPermission(
                            Manifest.permission.POST_NOTIFICATIONS
                    )
                            != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        1001
                );
            }
        }
    }


    // =========================================================
    // GET FCM TOKEN
    // =========================================================

    private void getFCMToken() {

        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        // لا نعرض أي رسالة للمستخدم
                        return;
                    }

                    String token =
                            task.getResult();

                    if (
                            token == null ||
                            token.trim().isEmpty()
                    ) {

                        return;
                    }

                    // حفظ Token في Supabase
                    sendTokenToSupabase(token);
                });
    }


    // =========================================================
    // SEND TOKEN TO SUPABASE
    // =========================================================

    private void sendTokenToSupabase(
            String token
    ) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url =
                        new URL(
                                SUPABASE_RPC_URL
                        );

                connection =
                        (HttpURLConnection)
                                url.openConnection();


                // -------------------------------------------------
                // HTTP
                // -------------------------------------------------

                connection.setRequestMethod(
                        "POST"
                );

                connection.setConnectTimeout(
                        15000
                );

                connection.setReadTimeout(
                        15000
                );

                connection.setUseCaches(
                        false
                );

                connection.setDoInput(
                        true
                );

                connection.setDoOutput(
                        true
                );


                // -------------------------------------------------
                // SUPABASE HEADERS
                // -------------------------------------------------

                connection.setRequestProperty(
                        "apikey",
                        SUPABASE_PUBLISHABLE_KEY
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );


                // -------------------------------------------------
                // RPC JSON
                // -------------------------------------------------

                String json =
                        "{"
                                + "\"p_token\":\""
                                + escapeJson(token)
                                + "\","
                                + "\"p_platform\":\"android\""
                                + "}";


                // -------------------------------------------------
                // SEND
                // -------------------------------------------------

                OutputStream outputStream =
                        connection.getOutputStream();

                outputStream.write(
                        json.getBytes("UTF-8")
                );

                outputStream.flush();

                outputStream.close();


                // -------------------------------------------------
                // READ RESPONSE
                // -------------------------------------------------

                int responseCode =
                        connection.getResponseCode();

                InputStream inputStream;

                if (
                        responseCode >= 200 &&
                        responseCode < 400
                ) {

                    inputStream =
                            connection.getInputStream();

                } else {

                    inputStream =
                            connection.getErrorStream();
                }

                /*
                 * نقرأ الرد فقط حتى لا يبقى الاتصال مفتوحًا.
                 * لا نعرضه داخل التطبيق.
                 */

                readStream(
                        inputStream
                );

            } catch (Exception ignored) {

                /*
                 * لا تظهر أخطاء Supabase للمستخدم.
                 */

            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }

        }).start();
    }


    // =========================================================
    // ESCAPE JSON
    // =========================================================

    private String escapeJson(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                )
                .replace(
                        "\t",
                        "\\t"
                );
    }


    // =========================================================
    // READ STREAM
    // =========================================================

    private String readStream(
            InputStream inputStream
    ) {

        if (inputStream == null) {

            return "";
        }

        StringBuilder result =
                new StringBuilder();

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    inputStream,
                                    "UTF-8"
                            )
                    );

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                result.append(line);
                result.append("\n");
            }

            reader.close();

        } catch (Exception ignored) {

            return "";
        }

        return result
                .toString()
                .trim();
    }


    // =========================================================
    // BACK BUTTON
    // =========================================================

    @Override
    public void onBackPressed() {

        if (
                webView != null &&
                webView.canGoBack()
        ) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}

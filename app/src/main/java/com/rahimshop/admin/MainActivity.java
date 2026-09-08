package com.rahimshop.admin;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String TAG = "RahimShopFCM";

    private static final String WEBSITE_URL =
            "https://shop-dz.gt.tc/admin/login.php";

    private static final String SUPABASE_URL =
            "https://tkntrbjsdxhizebascai.supabase.co";

    private static final String SUPABASE_PUBLISHABLE_KEY =
            "sb_publishable_P5ElVLCeoQrLdrbfI7G6Qg_hVR51Evo";

    private static final String SUPABASE_RPC_URL =
            SUPABASE_URL + "/rest/v1/rpc/save_fcm_token";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createInterface();

        setupWebView();

        /*
         * الحصول على الـToken الحالي في الخلفية.
         *
         * لا تظهر أي رسالة للمستخدم.
         */
        getFCMToken();
    }

    private void createInterface() {

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setGravity(
                Gravity.CENTER
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        webView = new WebView(this);

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        setContentView(root);
    }

    private void setupWebView() {

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        /*
         * WebChromeClient ضروري لبعض وظائف الموقع
         * مثل JavaScript dialogs وبعض المحتويات.
         */
        webView.setWebChromeClient(
                new WebChromeClient()
        );

        webView.setWebViewClient(
                new WebViewClient()
        );

        webView.loadUrl(
                WEBSITE_URL
        );
    }

    /*
     * =========================================================
     * FCM TOKEN
     * =========================================================
     */

    private void getFCMToken() {

        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        Log.e(
                                TAG,
                                "Failed to get FCM token",
                                task.getException()
                        );

                        return;
                    }

                    String token =
                            task.getResult();

                    if (token == null ||
                            token.trim().isEmpty()) {

                        Log.e(
                                TAG,
                                "FCM token is empty"
                        );

                        return;
                    }

                    Log.d(
                            TAG,
                            "Current FCM token obtained. Length = "
                                    + token.length()
                    );

                    /*
                     * حفظ الـToken الحالي في Supabase.
                     *
                     * RPC يستخدم ON CONFLICT لذلك إذا كان
                     * الـToken موجودًا سيتم تحديثه فقط.
                     */
                    sendTokenToSupabase(token);
                });
    }

    /*
     * =========================================================
     * SEND TOKEN TO SUPABASE
     * =========================================================
     */

    public static void sendTokenToSupabase(
            String token
    ) {

        if (token == null ||
                token.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "Cannot send empty FCM token"
            );

            return;
        }

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

                connection.setRequestProperty(
                        "apikey",
                        SUPABASE_PUBLISHABLE_KEY
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                /*
                 * RPC:
                 *
                 * save_fcm_token(
                 *     p_token,
                 *     p_platform
                 * )
                 */
                String json =
                        "{"
                                + "\"p_token\":\""
                                + escapeJson(token)
                                + "\","
                                + "\"p_platform\":\"android\""
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

                InputStream inputStream;

                if (responseCode >= 200 &&
                        responseCode < 400) {

                    inputStream =
                            connection.getInputStream();

                } else {

                    inputStream =
                            connection.getErrorStream();
                }

                String response =
                        readStream(
                                inputStream
                        );

                if (responseCode >= 200 &&
                        responseCode < 300) {

                    Log.d(
                            TAG,
                            "FCM token saved successfully. HTTP "
                                    + responseCode
                                    + " Response: "
                                    + response
                    );

                } else {

                    Log.e(
                            TAG,
                            "Failed to save FCM token. HTTP "
                                    + responseCode
                                    + " Response: "
                                    + response
                    );
                }

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Supabase token error",
                        e
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    /*
     * =========================================================
     * JSON ESCAPE
     * =========================================================
     */

    private static String escapeJson(
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

    /*
     * =========================================================
     * READ SERVER RESPONSE
     * =========================================================
     */

    private static String readStream(
            InputStream inputStream
    ) {

        if (inputStream == null) {

            return "No server response";
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

        } catch (Exception e) {

            return "Failed to read response: "
                    + e.getMessage();
        }

        return result
                .toString()
                .trim();
    }

    /*
     * =========================================================
     * BACK BUTTON
     * =========================================================
     */

    @Override
    public void onBackPressed() {

        if (webView != null &&
                webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    /*
     * =========================================================
     * CLEANUP
     * =========================================================
     */

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();

            webView.setWebChromeClient(
                    null
            );

            webView.setWebViewClient(
                    null
            );

            webView.destroy();

            webView = null;
        }

        super.onDestroy();
    }
}

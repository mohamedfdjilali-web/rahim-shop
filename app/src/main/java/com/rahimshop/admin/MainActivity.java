package com.rahimshop.admin;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String WEBSITE_URL =
            "https://shop-dz.gt.tc/admin/login.php";

    private static final String SUPABASE_URL =
            "https://tkntrbjsdxhizebascai.supabase.co";

    private static final String SUPABASE_PUBLISHABLE_KEY =
            "sb_publishable_P5ElVLCeoQrLdrbfI7G6Qg_hVR51Evo";

    private static final String SUPABASE_RPC_URL =
            SUPABASE_URL + "/rest/v1/rpc/save_fcm_token";

    private WebView webView;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createInterface();
        setupWebView();

        /*
         * الحصول على FCM Token في الخلفية.
         *
         * لا نعرض للمستخدم:
         * - جاري الحصول على Token
         * - تم الحصول على Token
         * - جاري الإرسال إلى Supabase
         *
         * التطبيق سيظهر فقط الموقع.
         */
        getFCMToken();
    }

    private void createInterface() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.WHITE
        );

        /*
         * شريط الحالة مخفي.
         *
         * نحتفظ به داخليًا حتى نستطيع عرض
         * أخطاء مهمة فقط إذا حدثت مشكلة.
         */
        statusText =
                new TextView(this);

        statusText.setVisibility(
                TextView.GONE
        );

        root.addView(
                statusText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        webView =
                new WebView(this);

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
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

        /*
         * السماح بالـ JavaScript dialogs
         * والمحتوى المتوافق مع الموقع.
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

                        /*
                         * لا نعرض الخطأ للمستخدم.
                         * فقط Logcat للمطور.
                         */
                        android.util.Log.e(
                                "RahimShopFCM",
                                "Failed to get FCM token",
                                task.getException()
                        );

                        return;
                    }

                    String token =
                            task.getResult();

                    if (token == null
                            || token.trim().isEmpty()) {

                        android.util.Log.e(
                                "RahimShopFCM",
                                "FCM token is empty"
                        );

                        return;
                    }

                    android.util.Log.d(
                            "RahimShopFCM",
                            "FCM token obtained. Length = "
                                    + token.length()
                    );

                    /*
                     * حفظ آخر Token في Supabase.
                     */
                    sendTokenToSupabase(token);
                });
    }

    /*
     * =========================================================
     * SEND TOKEN TO SUPABASE
     * =========================================================
     */

    private void sendTokenToSupabase(
            String token
    ) {

        new Thread(() -> {

            HttpURLConnection connection =
                    null;

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

                /*
                 * Supabase Publishable Key
                 */
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
                 * RPC parameters
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

                if (responseCode >= 200
                        && responseCode < 400) {

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

                if (responseCode >= 200
                        && responseCode < 300) {

                    android.util.Log.d(
                            "RahimShopFCM",
                            "FCM token saved successfully. HTTP "
                                    + responseCode
                                    + " Response: "
                                    + response
                    );

                } else {

                    android.util.Log.e(
                            "RahimShopFCM",
                            "Failed to save FCM token. HTTP "
                                    + responseCode
                                    + " Response: "
                                    + response
                    );
                }

            } catch (Exception e) {

                /*
                 * الخطأ يظهر فقط في Logcat.
                 * لا نزعج المستخدم برسالة على الشاشة.
                 */
                android.util.Log.e(
                        "RahimShopFCM",
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

    /*
     * =========================================================
     * READ SERVER RESPONSE
     * =========================================================
     */

    private String readStream(
            InputStream inputStream
    ) {

        if (inputStream == null) {

            return "لا يوجد رد من السيرفر";
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

            return "Failed to read server response: "
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

        if (webView != null
                && webView.canGoBack()) {

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
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();

            webView = null;
        }

        super.onDestroy();
    }
}

package com.rahimshop.admin;

import android.app.Activity;
import android.os.Bundle;
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

        /*
         * إنشاء WebView فقط
         * بدون أي شريط رسائل أو Status Text
         */
        webView = new WebView(this);

        setContentView(webView);

        setupWebView();

        /*
         * الحصول على FCM Token وحفظه في Supabase
         * في الخلفية بدون إظهار أي رسالة للمستخدم.
         */
        getFCMToken();
    }

    /*
     * ---------------------------------------------------------
     * WEBVIEW
     * ---------------------------------------------------------
     */

    private void setupWebView() {

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);

        settings.setDomStorageEnabled(true);

        settings.setDatabaseEnabled(true);

        settings.setLoadWithOverviewMode(true);

        settings.setUseWideViewPort(true);

        /*
         * السماح بالكوكيز مهم لتسجيل الدخول
         * وحفظ جلسة الإدارة.
         */
        android.webkit.CookieManager
                .getInstance()
                .setAcceptCookie(true);

        android.webkit.CookieManager
                .getInstance()
                .setAcceptThirdPartyCookies(
                        webView,
                        true
                );

        /*
         * إبقاء الروابط داخل WebView.
         */
        webView.setWebViewClient(
                new WebViewClient()
        );

        /*
         * فتح الموقع.
         */
        webView.loadUrl(
                WEBSITE_URL
        );
    }

    /*
     * ---------------------------------------------------------
     * FCM TOKEN
     * ---------------------------------------------------------
     */

    private void getFCMToken() {

        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnCompleteListener(task -> {

                    /*
                     * إذا فشل الحصول على Token
                     * لا نعرض أي رسالة للمستخدم.
                     */
                    if (!task.isSuccessful()) {

                        return;
                    }

                    String token =
                            task.getResult();

                    /*
                     * التأكد من أن Token صالح.
                     */
                    if (token == null
                            || token.trim().isEmpty()) {

                        return;
                    }

                    /*
                     * إرسال Token إلى Supabase
                     * في Thread مستقل.
                     */
                    sendTokenToSupabase(
                            token
                    );
                });
    }

    /*
     * ---------------------------------------------------------
     * SEND TOKEN TO SUPABASE
     * ---------------------------------------------------------
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

                /*
                 * لا نضع Service Role Key
                 * داخل تطبيق Android.
                 */
                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                /*
                 * JSON الخاص بالـRPC.
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

                /*
                 * قراءة الرد فقط للتشخيص الداخلي.
                 * لن يظهر للمستخدم.
                 */
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

                /*
                 * قراءة الرد حتى لا يبقى الاتصال مفتوحًا.
                 */
                readStream(
                        inputStream
                );

            } catch (Exception ignored) {

                /*
                 * لا نعرض أي Error للمستخدم.
                 *
                 * FCM سيستمر بالعمل حتى لو فشل
                 * حفظ Token في هذه المحاولة.
                 */

            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }

        }).start();
    }

    /*
     * ---------------------------------------------------------
     * JSON ESCAPE
     * ---------------------------------------------------------
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
     * ---------------------------------------------------------
     * READ SERVER RESPONSE
     * ---------------------------------------------------------
     */

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
            }

            reader.close();

        } catch (Exception ignored) {

            return "";
        }

        return result.toString();
    }

    /*
     * ---------------------------------------------------------
     * BACK BUTTON
     * ---------------------------------------------------------
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
     * ---------------------------------------------------------
     * DESTROY WEBVIEW
     * ---------------------------------------------------------
     */

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();

            webView.setWebViewClient(null);

            webView.destroy();

            webView = null;
        }

        super.onDestroy();
    }
}

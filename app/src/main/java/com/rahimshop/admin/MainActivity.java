package com.rahimshop.admin;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.webkit.WebChromeClient;

import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    /*
     * ============================================================
     * CONFIG
     * ============================================================
     */

    private static final String WEBSITE_URL =
            "https://shop-dz.gt.tc/admin/login.php";

    private static final String SUPABASE_URL =
            "https://tkntrbjsdxhizebascai.supabase.co";

    private static final String SUPABASE_PUBLISHABLE_KEY =
            "sb_publishable_P5ElVLCeoQrLdrbfI7G6Qg_hVR51Evo";

    /*
     * RPC الخاصة بحفظ FCM Token
     */
    private static final String SUPABASE_RPC_URL =
            SUPABASE_URL +
            "/rest/v1/rpc/save_fcm_token";

    private WebView webView;


    /*
     * ============================================================
     * ACTIVITY
     * ============================================================
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        /*
         * إنشاء WebView فقط
         *
         * لا يوجد TextView
         * لا توجد رسائل تشخيصية
         */
        createInterface();

        /*
         * فتح لوحة SHOP-DZ
         */
        setupWebView();

        /*
         * الحصول على FCM Token
         * وإرساله إلى Supabase في الخلفية
         */
        getFCMToken();
    }


    /*
     * ============================================================
     * INTERFACE
     * ============================================================
     */

    private void createInterface() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        /*
         * WebView
         */
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


    /*
     * ============================================================
     * WEBVIEW
     * ============================================================
     */

    private void setupWebView() {

    WebSettings settings =
            webView.getSettings();

    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setDatabaseEnabled(true);

    settings.setLoadWithOverviewMode(true);
    settings.setUseWideViewPort(true);

    settings.setJavaScriptCanOpenWindowsAutomatically(true);
    settings.setSupportMultipleWindows(false);

    webView.setWebViewClient(
            new WebViewClient()
    );

    webView.setWebChromeClient(
            new WebChromeClient()
    );

    addStatus(
            "جاري فتح SHOP-DZ...",
            false
    );

    webView.loadUrl(
            WEBSITE_URL
    );
}

    /*
     * ============================================================
     * FCM TOKEN
     * ============================================================
     */

    private void getFCMToken() {

        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnCompleteListener(task -> {

                    /*
                     * إذا فشل الحصول على Token
                     *
                     * لا نظهر أي رسالة للمستخدم.
                     */
                    if (!task.isSuccessful()) {

                        return;
                    }

                    String token =
                            task.getResult();

                    /*
                     * التحقق من Token
                     */
                    if (
                            token == null ||
                            token.trim().isEmpty()
                    ) {

                        return;
                    }

                    /*
                     * إرسال Token إلى Supabase
                     */
                    sendTokenToSupabase(
                            token
                    );
                });
    }


    /*
     * ============================================================
     * SEND TOKEN TO SUPABASE RPC
     * ============================================================
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

                /*
                 * POST
                 */
                connection.setRequestMethod(
                        "POST"
                );

                /*
                 * Timeouts
                 */
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
                 * ====================================================
                 * SUPABASE HEADERS
                 * ====================================================
                 */

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


                /*
                 * ====================================================
                 * JSON
                 * ====================================================
                 */

                String json =
                        "{"
                                + "\"p_token\":\""
                                + escapeJson(token)
                                + "\","
                                + "\"p_platform\":\"android\""
                                + "}";


                /*
                 * إرسال JSON
                 */
                OutputStream outputStream =
                        connection.getOutputStream();

                outputStream.write(
                        json.getBytes("UTF-8")
                );

                outputStream.flush();

                outputStream.close();


                /*
                 * الحصول على HTTP Code
                 */
                int responseCode =
                        connection.getResponseCode();


                /*
                 * قراءة الرد
                 *
                 * لا نعرضه للمستخدم.
                 */
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
                 * قراءة الرد فقط للتشخيص الداخلي
                 */
                readStream(
                        inputStream
                );


            } catch (Exception ignored) {

                /*
                 * لا نظهر أي Error للمستخدم.
                 *
                 * الإشعارات نفسها لا تعتمد على هذه الشاشة.
                 */

            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }

        }).start();
    }


    /*
     * ============================================================
     * JSON ESCAPE
     * ============================================================
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
     * ============================================================
     * READ SERVER RESPONSE
     * ============================================================
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
                    (line =
                            reader.readLine())
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


    /*
     * ============================================================
     * BACK BUTTON
     * ============================================================
     */

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

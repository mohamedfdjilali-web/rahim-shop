package com.rahimshop.admin;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
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

    // Supabase
    private static final String SUPABASE_URL =
            "https://tkntrbjsdxhizebascai.supabase.co";

    private static final String SUPABASE_PUBLISHABLE_KEY =
            "sb_publishable_P5ElVLCeoQrLdrbfI7G6Qg_hVR51Evo";

    private static final String SUPABASE_TOKEN_URL =
            SUPABASE_URL + "/rest/v1/push_tokens";

    private TextView statusText;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createInterface();

        addStatus("تشغيل التطبيق...", false);

        setupWebView();

        getFirebaseToken();
    }

    private void createInterface() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        statusText = new TextView(this);

        statusText.setText("جاري تشغيل التطبيق...");
        statusText.setTextSize(16f);
        statusText.setTextColor(Color.BLACK);
        statusText.setTypeface(null, Typeface.BOLD);
        statusText.setGravity(Gravity.CENTER_VERTICAL);

        statusText.setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(12)
        );

        root.addView(
                statusText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        webView = new WebView(this);

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

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient());

        addStatus("جاري فتح الموقع...", false);

        webView.loadUrl(WEBSITE_URL);
    }

    private void getFirebaseToken() {

        addStatus(
                "جاري الحصول على FCM Token...",
                false
        );

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        addStatus(
                                "❌ فشل الحصول على FCM Token\n\n"
                                        + "الخطأ:\n"
                                        + task.getException(),
                                true
                        );

                        return;
                    }

                    String token = task.getResult();

                    if (token == null || token.trim().isEmpty()) {

                        addStatus(
                                "❌ FCM Token فارغ",
                                true
                        );

                        return;
                    }

                    addStatus(
                            "✅ تم الحصول على FCM Token\n\n"
                                    + "طول Token = "
                                    + token.length()
                                    + "\n\n"
                                    + "جاري الإرسال إلى Supabase...",
                            false
                    );

                    sendTokenToSupabase(token);
                });
    }

    private void sendTokenToSupabase(String token) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                addStatus(
                        "📡 جاري الاتصال بـ Supabase...\n\n"
                                + "Token length = "
                                + token.length(),
                        false
                );

                URL url =
                        new URL(SUPABASE_TOKEN_URL);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");

                connection.setConnectTimeout(15000);

                connection.setReadTimeout(15000);

                connection.setUseCaches(false);

                connection.setDoInput(true);

                connection.setDoOutput(true);

                // Supabase Publishable Key
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

                String json =
                        "{"
                                + "\"token\":\""
                                + escapeJson(token)
                                + "\","
                                + "\"platform\":\"android\","
                                + "\"is_active\":true"
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

                String serverResponse =
                        readStream(inputStream);

                String result =
                        "📡 نتيجة Supabase\n\n"
                                + "HTTP Code: "
                                + responseCode
                                + "\n\n"
                                + "Server Response:\n"
                                + serverResponse;

                if (responseCode >= 200
                        && responseCode < 300) {

                    addStatus(
                            "✅ تم حفظ FCM Token في Supabase بنجاح\n\n"
                                    + result,
                            false
                    );

                } else {

                    addStatus(
                            "❌ Supabase رفض الطلب\n\n"
                                    + result,
                            true
                    );
                }

            } catch (Exception e) {

                addStatus(
                        "❌ خطأ أثناء الاتصال بـ Supabase\n\n"
                                + e.getClass().getSimpleName()
                                + "\n\n"
                                + e.getMessage(),
                        true
                );

            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }

        }).start();
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String readStream(InputStream inputStream) {

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

            while ((line = reader.readLine()) != null) {

                result.append(line);
                result.append("\n");
            }

            reader.close();

        } catch (Exception e) {

            return "فشل قراءة رد السيرفر:\n"
                    + e.getMessage();
        }

        return result.toString().trim();
    }

    private void addStatus(
            String message,
            boolean error
    ) {

        runOnUiThread(() -> {

            if (statusText == null) {
                return;
            }

            statusText.setText(message);

            if (error) {

                statusText.setTextColor(
                        Color.rgb(180, 0, 0)
                );

            } else {

                statusText.setTextColor(
                        Color.rgb(0, 100, 50)
                );
            }
        });
    }

    private int dp(int value) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return (int)
                (value * density + 0.5f);
    }

    @Override
    public void onBackPressed() {

        if (webView != null
                && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}

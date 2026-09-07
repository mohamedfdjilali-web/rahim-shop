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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    private static final String WEBSITE_URL =
            "https://shop-dz.gt.tc/admin/login.php";

    private static final String TOKEN_URL =
            "https://shop-dz.gt.tc/admin/save_push_token_get.php";

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

        // عنوان الحالة
        statusText = new TextView(this);

        statusText.setText("جاري تشغيل التطبيق...");
        statusText.setTextSize(16f);
        statusText.setTextColor(Color.BLACK);
        statusText.setTypeface(null, Typeface.BOLD);
        statusText.setGravity(Gravity.CENTER_VERTICAL);

        int padding = dp(12);

        statusText.setPadding(
                dp(12),
                padding,
                dp(12),
                padding
        );

        root.addView(
                statusText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // WebView
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
                                        + "الخطأ: "
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

                    final String finalToken = token;

                    addStatus(
                            "✅ تم الحصول على FCM Token\n\n"
                                    + "طول Token = "
                                    + finalToken.length()
                                    + "\n\n"
                                    + "جاري الإرسال إلى السيرفر...",
                            false
                    );

                    sendTokenToServer(finalToken);
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
                        TOKEN_URL
                                + "?token="
                                + encodedToken;

                addStatus(
                        "📡 جاري الاتصال بالسيرفر...\n\n"
                                + "Token length = "
                                + token.length(),
                        false
                );

                URL url = new URL(urlString);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");

                connection.setConnectTimeout(15000);

                connection.setReadTimeout(15000);

                connection.setUseCaches(false);

                connection.setDoInput(true);

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
                        "📡 نتيجة إرسال FCM Token\n\n"
                                + "HTTP Code: "
                                + responseCode
                                + "\n\n"
                                + "Server Response:\n"
                                + serverResponse;

                if (responseCode >= 200
                        && responseCode < 300) {

                    addStatus(
                            "✅ تم إرسال FCM Token بنجاح\n\n"
                                    + result,
                            false
                    );

                } else {

                    addStatus(
                            "❌ السيرفر رفض الطلب\n\n"
                                    + result,
                            true
                    );
                }

            } catch (Exception e) {

                addStatus(
                        "❌ خطأ أثناء إرسال Token\n\n"
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
                                    inputStream
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

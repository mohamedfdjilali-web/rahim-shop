package com.rahimshop.admin;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.TextView;

import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private TextView status;

    private static final String TOKEN_URL =
        "https://shop-dz.gt.tc/admin/test_post.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        status = new TextView(this);

        status.setTextSize(18);
        status.setTextColor(Color.BLACK);
        status.setGravity(Gravity.CENTER);
        status.setPadding(30, 30, 30, 30);

        setContentView(status);

        status.setText(
                "Rahim Shop\n\n" +
                "جاري الحصول على FCM Token..."
        );

        getFCMToken();
    }

    private void getFCMToken() {

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        status.setText(
                                "Firebase Token ERROR\n\n" +
                                String.valueOf(task.getException())
                        );

                        return;
                    }

                    String token = task.getResult();

                    if (token == null || token.isEmpty()) {

                        status.setText(
                                "Firebase أعاد Token فارغ"
                        );

                        return;
                    }

                    status.setText(
                            "FCM Token تم الحصول عليه ✅\n\n" +
                            "طول Token: " +
                            token.length() +
                            "\n\nجاري الإرسال للسيرفر..."
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

                String json =
                        "{"
                        + "\"token\":\""
                        + token
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        + "\","
                        + "\"platform\":\"android\""
                        + "}";

                OutputStream output =
                        connection.getOutputStream();

                output.write(
                        json.getBytes("UTF-8")
                );

                output.flush();
                output.close();

                int code =
                        connection.getResponseCode();

                BufferedReader reader;

                if (code >= 200 && code < 400) {

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

                String result =
                        "HTTP: " + code +
                        "\n\n" +
                        response.toString();

                runOnUiThread(() ->
                        status.setText(result)
                );

            } catch (Exception e) {

                runOnUiThread(() ->
                        status.setText(
                                "خطأ في الاتصال بالسيرفر:\n\n" +
                                e.toString()
                        )
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }
}

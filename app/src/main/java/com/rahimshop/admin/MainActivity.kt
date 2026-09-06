package com.rahimshop.admin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // طلب إذن الإشعارات في Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // WebView
        webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true

        webView.webViewClient = WebViewClient()

        setContentView(webView)

        // فتح لوحة الإدارة
        webView.loadUrl(
            "https://shop-dz.gt.tc/admin/"
        )

        // الحصول على FCM Token
        getFirebaseToken()
    }

    private fun getFirebaseToken() {

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    return@addOnCompleteListener
                }

                val token = task.result

                // إرسال Token إلى السيرفر
                sendTokenToServer(token)

                // إرسال Token إلى JavaScript
                sendTokenToWebView(token)
            }
    }

    private fun sendTokenToServer(token: String) {

        thread {

            try {

                val url = URL(
                    "https://shop-dz.gt.tc/admin/save_push_token.php"
                )

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                connection.doOutput = true

                val json =
                    """
                    {
                        "token": "$token",
                        "platform": "android"
                    }
                    """.trimIndent()

                connection.outputStream.use { output ->

                    output.write(
                        json.toByteArray(
                            Charsets.UTF_8
                        )
                    )
                }

                connection.responseCode

                connection.disconnect()

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    private fun sendTokenToWebView(token: String) {

        runOnUiThread {

            val escapedToken =
                token
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")

            webView.evaluateJavascript(
                """
                window.dispatchEvent(
                    new CustomEvent(
                        'SHOP_DZ_FCM_TOKEN',
                        {
                            detail: {
                                token: '$escapedToken',
                                platform: 'android'
                            }
                        }
                    )
                );
                """.trimIndent(),
                null
            )
        }
    }

    override fun onBackPressed() {

        if (webView.canGoBack()) {

            webView.goBack()

        } else {

            super.onBackPressed()
        }
    }
}

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

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // طلب إذن الإشعارات Android 13+
        requestNotificationPermission()

        // إنشاء WebView
        webView = WebView(this)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            allowFileAccess = false
            allowContentAccess = false

            javaScriptCanOpenWindowsAutomatically = false
        }

        webView.webViewClient = WebViewClient()

        setContentView(webView)

        // الرابط الافتراضي للوحة الإدارة
        val notificationUrl =
            intent.getStringExtra("notification_url")

        if (!notificationUrl.isNullOrEmpty()) {

            openNotificationUrl(notificationUrl)

        } else {

            webView.loadUrl(
                "https://shop-dz.gt.tc/admin/login.php"
            )
        }

        // الحصول على FCM Token
        getFirebaseToken()
    }

    private fun requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    1001
                )
            }
        }
    }

    private fun getFirebaseToken() {

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {

                    android.util.Log.e(
                        "SHOP_DZ_FCM",
                        "FCM Token failed",
                        task.exception
                    )

                    return@addOnCompleteListener
                }

                val token = task.result

                android.util.Log.d(
                    "SHOP_DZ_FCM",
                    "FCM Token received"
                )

                sendTokenToServer(token)
            }
    }

    private fun sendTokenToServer(token: String) {

        Thread {

            var connection: HttpURLConnection? = null

            try {

                val url = URL(
                    "https://shop-dz.gt.tc/admin/save_push_token.php"
                )

                connection =
                    url.openConnection()
                        as HttpURLConnection

                connection.requestMethod = "POST"

                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val escapedToken =
                    token
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")

                val json =
                    """
                    {
                        "token": "$escapedToken",
                        "platform": "android"
                    }
                    """.trimIndent()

                connection.outputStream.use { output ->

                    output.write(
                        json.toByteArray(
                            Charsets.UTF_8
                        )
                    )

                    output.flush()
                }

                val responseCode =
                    connection.responseCode

                android.util.Log.d(
                    "SHOP_DZ_FCM",
                    "Token upload response: $responseCode"
                )

                connection.disconnect()

            } catch (e: Exception) {

                android.util.Log.e(
                    "SHOP_DZ_FCM",
                    "Token upload failed",
                    e
                )

                connection?.disconnect()
            }

        }.start()
    }

    private fun openNotificationUrl(url: String) {

        val finalUrl: String

        if (url.startsWith("http://") ||
            url.startsWith("https://")
        ) {

            finalUrl = url

        } else {

            finalUrl =
                "https://shop-dz.gt.tc" +
                if (url.startsWith("/")) {
                    url
                } else {
                    "/$url"
                }
        }

        webView.loadUrl(finalUrl)
    }

    override fun onNewIntent(intent: android.content.Intent?) {

        super.onNewIntent(intent)

        if (intent == null) {
            return
        }

        setIntent(intent)

        val notificationUrl =
            intent.getStringExtra(
                "notification_url"
            )

        if (!notificationUrl.isNullOrEmpty()) {

            openNotificationUrl(
                notificationUrl
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

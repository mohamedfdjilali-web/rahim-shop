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

        // إنشاء WebView
        webView = WebView(this)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
        }

        webView.webViewClient = WebViewClient()

        setContentView(webView)

        // فتح لوحة الإدارة
        webView.loadUrl("https://shop-dz.gt.tc/admin/")

        // طلب إذن الإشعارات
        requestNotificationPermission()

        // الحصول على Firebase Token
        getFirebaseToken()

        // إذا فتح التطبيق من إشعار
        handleNotificationIntent()
    }

    private fun requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= 33) {

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
    }

    private fun getFirebaseToken() {

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    return@addOnCompleteListener
                }

                val token = task.result

                if (!token.isNullOrEmpty()) {
                    sendTokenToServer(token)
                }
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

                val json =
                    """
                    {
                        "token": "$token",
                        "platform": "android"
                    }
                    """.trimIndent()

                connection.outputStream.use { outputStream ->

                    outputStream.write(
                        json.toByteArray(Charsets.UTF_8)
                    )

                    outputStream.flush()
                }

                connection.responseCode

                connection.disconnect()

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    private fun handleNotificationIntent() {

        val notificationUrl =
            intent.getStringExtra("notification_url")

        if (!notificationUrl.isNullOrEmpty()) {

            if (notificationUrl.startsWith("https://")) {

                webView.loadUrl(notificationUrl)

            } else if (notificationUrl.startsWith("/")) {

                webView.loadUrl(
                    "https://shop-dz.gt.tc$notificationUrl"
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {

        super.onNewIntent(intent)

        if (intent != null) {

            setIntent(intent)

            val notificationUrl =
                intent.getStringExtra("notification_url")

            if (!notificationUrl.isNullOrEmpty()) {

                if (notificationUrl.startsWith("https://")) {

                    webView.loadUrl(notificationUrl)

                } else if (notificationUrl.startsWith("/")) {

                    webView.loadUrl(
                        "https://shop-dz.gt.tc$notificationUrl"
                    )
                }
            }
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

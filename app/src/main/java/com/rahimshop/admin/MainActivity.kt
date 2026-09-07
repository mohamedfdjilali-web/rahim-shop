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

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Notification permission
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

        // Create WebView
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

        // Open admin panel
        webView.loadUrl(
            "https://shop-dz.gt.tc/admin/login.php"
        )

        // Get Firebase token
        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    return@addOnCompleteListener
                }

                val token = task.result

                sendTokenToServer(token)
            }
    }

    private fun sendTokenToServer(token: String) {

        Thread {

            try {

                val url = java.net.URL(
                    "https://shop-dz.gt.tc/admin/save_push_token.php"
                )

                val connection =
                    url.openConnection()
                        as java.net.HttpURLConnection

                connection.requestMethod = "POST"
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

                connection.outputStream.use { output ->
                    output.write(
                        json.toByteArray(Charsets.UTF_8)
                    )
                }

                connection.inputStream.close()
                connection.disconnect()

            } catch (e: Exception) {

                e.printStackTrace()
            }

        }.start()
    }

    override fun onBackPressed() {

        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

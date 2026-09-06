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

        webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true

        webView.webViewClient = WebViewClient()

        setContentView(webView)

        // فتح لوحة إدارة SHOP-DZ
        webView.loadUrl(
            "https://shop-dz.gt.tc/admin/"
        )

        // الحصول على FCM Token الحقيقي
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    return@addOnCompleteListener
                }

                val token = task.result

                // إرسال Token إلى JavaScript داخل WebView
                webView.evaluateJavascript(
                    """
                    window.dispatchEvent(
                        new CustomEvent(
                            'SHOP_DZ_FCM_TOKEN',
                            {
                                detail: {
                                    token: '$token',
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

package com.rahimshop.admin

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {

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
            webView.loadUrl(
                "https://shop-dz.gt.tc/admin/login.php"
            )

        } catch (e: Exception) {

            showError(e)

        } catch (e: Error) {

            showError(e)

        }
    }

    private fun showError(error: Throwable) {

        val message = TextView(this)

        message.text =
            "SHOP-DZ\n\n" +
            "حدث خطأ أثناء تشغيل التطبيق:\n\n" +
            error.javaClass.name +
            "\n\n" +
            (error.message ?: "لا توجد تفاصيل")

        message.textSize = 16f
        message.setTextColor(Color.BLACK)
        message.setPadding(40, 60, 40, 60)
        message.gravity = Gravity.CENTER

        setContentView(message)
    }

    override fun onBackPressed() {

        if (::webView.isInitialized && webView.canGoBack()) {

            webView.goBack()

        } else {

            super.onBackPressed()

        }
    }
}

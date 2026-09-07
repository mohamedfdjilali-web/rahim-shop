package com.rahimshop.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL

class ShopFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // إرسال الـ FCM Token الجديد إلى السيرفر
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title =
            message.notification?.title
                ?: message.data["title"]
                ?: "🔔 SHOP-DZ"

        val body =
            message.notification?.body
                ?: message.data["body"]
                ?: "لديك إشعار جديد"

        val url =
            message.data["url"]
                ?: "/admin/"

        showNotification(
            title,
            body,
            url
        )
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

    private fun showNotification(
        title: String,
        body: String,
        url: String
    ) {

        val channelId = "shop_dz_orders"

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )

        intent.putExtra(
            "notification_url",
            url
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_IMMUTABLE
                    } else {
                        0
                    }
            )

        val notificationManager =
            getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    channelId,
                    "SHOP-DZ Orders",
                    NotificationManager.IMPORTANCE_HIGH
                )

            channel.description =
                "إشعارات الطلبات الجديدة"

            notificationManager.createNotificationChannel(
                channel
            )
        }

        val notification =
            NotificationCompat.Builder(
                this,
                channelId
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(body)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}

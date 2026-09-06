package com.rahimshop.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ShopFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // سيتم إرسال Token إلى السيرفر من MainActivity
        // عند تشغيل التطبيق.
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

        showNotification(title, body, url)
    }

    private fun showNotification(
        title: String,
        body: String,
        url: String
    ) {

        val channelId = "shop_dz_orders"

        val intent = Intent(this, MainActivity::class.java)

        intent.putExtra("notification_url", url)

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pendingIntent = PendingIntent.getActivity(
            this,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PendingIntent.FLAG_IMMUTABLE
                else
                    0
        )

        val notificationManager =
            getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
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

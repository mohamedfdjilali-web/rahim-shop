```java
package com.rahimshop.admin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService
        extends FirebaseMessagingService {

    private static final String TAG =
            "RahimShopFCM";

    private static final String CHANNEL_ID =
            "rahim_shop_orders";

    @Override
    public void onMessageReceived(
            RemoteMessage remoteMessage
    ) {

        Log.d(
                TAG,
                "FCM message received"
        );

        String title =
                "🔔 طلب جديد";

        String body =
                "لديك طلب جديد في SHOP-DZ";

        /*
         * Notification payload
         */
        if (remoteMessage.getNotification() != null) {

            String notificationTitle =
                    remoteMessage
                            .getNotification()
                            .getTitle();

            String notificationBody =
                    remoteMessage
                            .getNotification()
                            .getBody();

            if (notificationTitle != null
                    && !notificationTitle.isEmpty()) {

                title = notificationTitle;
            }

            if (notificationBody != null
                    && !notificationBody.isEmpty()) {

                body = notificationBody;
            }
        }

        /*
         * Data payload
         */
        if (!remoteMessage
                .getData()
                .isEmpty()) {

            String dataTitle =
                    remoteMessage
                            .getData()
                            .get("title");

            String dataBody =
                    remoteMessage
                            .getData()
                            .get("body");

            if (dataTitle != null
                    && !dataTitle.isEmpty()) {

                title = dataTitle;
            }

            if (dataBody != null
                    && !dataBody.isEmpty()) {

                body = dataBody;
            }
        }

        showNotification(
                title,
                body
        );
    }

    private void showNotification(
            String title,
            String body
    ) {

        createNotificationChannel();

        /*
         * فتح MainActivity عند الضغط على الإشعار
         */
        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        /*
         * إنشاء الإشعار
         *
         * مهم:
         * ic_notification هو رمز الإشعار
         * وليس أيقونة التطبيق الرئيسية.
         */
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )

                        .setSmallIcon(
                                R.drawable.ic_notification
                        )

                        .setContentTitle(
                                title
                        )

                        .setContentText(
                                body
                        )

                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(body)
                        )

                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH
                        )

                        .setAutoCancel(
                                true
                        )

                        .setContentIntent(
                                pendingIntent
                        )

                        .setCategory(
                                NotificationCompat.CATEGORY_MESSAGE
                        );

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager != null) {

            int notificationId =
                    (int) System.currentTimeMillis();

            manager.notify(
                    notificationId,
                    builder.build()
            );
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "طلبات SHOP-DZ",
                            NotificationManager
                                    .IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "إشعارات الطلبات الجديدة"
            );

            channel.enableVibration(true);

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }

    @Override
    public void onNewToken(
            String token
    ) {

        super.onNewToken(token);

        Log.d(
                TAG,
                "New FCM Token: " + token
        );

        /*
         * MainActivity يتعامل حاليًا مع
         * حفظ الـToken في Supabase.
         *
         * لا نغير هذا الجزء الآن.
         */
    }
}
```

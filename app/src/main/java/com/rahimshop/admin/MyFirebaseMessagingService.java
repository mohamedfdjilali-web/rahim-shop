package com.rahimshop.admin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService
        extends FirebaseMessagingService {

    private static final String TAG =
            "RahimShopFCM";

    private static final String CHANNEL_ID =
            "rahim_shop_orders";

    /*
     * =========================================================
     * RECEIVE MESSAGE
     * =========================================================
     */

    @Override
    public void onMessageReceived(
            @NonNull RemoteMessage remoteMessage
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

            if (notificationTitle != null &&
                    !notificationTitle.isEmpty()) {

                title =
                        notificationTitle;
            }

            if (notificationBody != null &&
                    !notificationBody.isEmpty()) {

                body =
                        notificationBody;
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

            if (dataTitle != null &&
                    !dataTitle.isEmpty()) {

                title =
                        dataTitle;
            }

            if (dataBody != null &&
                    !dataBody.isEmpty()) {

                body =
                        dataBody;
            }
        }

        /*
         * إظهار الإشعار عندما تصل الرسالة
         * إلى onMessageReceived.
         */
        showNotification(
                title,
                body
        );
    }

    /*
     * =========================================================
     * NEW FCM TOKEN
     * =========================================================
     */

    @Override
    public void onNewToken(
            @NonNull String token
    ) {

        super.onNewToken(token);

        Log.d(
                TAG,
                "New FCM token received. Length = "
                        + token.length()
        );

        /*
         * Firebase استدعى onNewToken لأن الـToken
         * جديد أو تغير.
         *
         * نرسله مباشرة إلى Supabase.
         */
        MainActivity.sendTokenToSupabase(
                token
        );
    }

    /*
     * =========================================================
     * SHOW NOTIFICATION
     * =========================================================
     */

    private void showNotification(
            String title,
            String body
    ) {

        createNotificationChannel();

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        1001,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        /*
         * أيقونة الإشعار.
         *
         * app_icon يجب أن يكون موجودًا داخل:
         *
         * app/src/main/res/drawable/app_icon.png
         *
         * إذا كانت أيقونتك الحالية تعمل بهذا الاسم
         * اتركها كما هي.
         */
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )

                        .setSmallIcon(
                                R.drawable.app_icon
                        )

                        .setContentTitle(
                                title
                        )

                        .setContentText(
                                body
                        )

                        .setStyle(
                                new NotificationCompat
                                        .BigTextStyle()
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

                        .setDefaults(
                                NotificationCompat.DEFAULT_ALL
                        );

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager != null) {

            int notificationId =
                    (int)
                            System.currentTimeMillis();

            manager.notify(
                    notificationId,
                    builder.build()
            );
        }
    }

    /*
     * =========================================================
     * NOTIFICATION CHANNEL
     * =========================================================
     */

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

            channel.enableVibration(
                    true
            );

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
}

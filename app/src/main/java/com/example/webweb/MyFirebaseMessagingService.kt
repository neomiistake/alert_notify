package com.example.webweb // <-- 這個要和你的專案套件名稱一樣

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // 這是用來在 Logcat 裡過濾訊息的標籤
    private val TAG = "MyFirebaseMsgService"

    /**
     * 當 App 收到新的推播通知時，這個方法會被呼叫。
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // 從訊息中取出標題和內容
        remoteMessage.notification?.let {
            val title = it.title
            val body = it.body
            Log.d(TAG, "Notification Title: $title")
            Log.d(TAG, "Notification Body: $body")

            // 呼叫我們自己寫的函式，來顯示這個通知
            sendNotification(title, body)
        }
    }

    /**
     * 當 App 第一次安裝或 Token 更新時，這個方法會被呼叫。
     * 我們就是要從這裡拿到要給 Python 用的「設備 Token」。
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        // 用最醒目的方式把 Token 印在 Logcat 裡，方便我們複製
        println("!!!!!!!!!!!!!!!!!! COPY THIS TOKEN !!!!!!!!!!!!!!!!!!")
        println(token)
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    }

    /**
     * 這個函式負責建立並顯示一個手機通知。
     */
    private fun sendNotification(title: String?, messageBody: String?) {
        // 1. 建立一個目標為我們 MapActivity 的 Intent
        val intent = Intent(this, MapActivity::class.java)

        // 這裡的 TaskStackBuilder 是關鍵，它能幫助我們建立一個正確的導航流程
        // 這樣當使用者在 MapActivity 按下返回鍵時，會回到手機的主畫面，而不是奇怪的頁面
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            // 加入一個返回堆疊，指向我們的 MapActivity
            addNextIntentWithParentStack(intent)
            // 取得最終的 PendingIntent
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val channelId = "danger_alert_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // ★★★ 把這個新的、更強大的 PendingIntent 綁定上去 ★★★

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // ... 後面的 Channel 建立程式碼不變 ...
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "危險警報通知",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
}
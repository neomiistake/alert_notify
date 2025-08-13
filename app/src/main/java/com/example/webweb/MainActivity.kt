package com.example.webweb

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging

class MainActivity : AppCompatActivity() {

    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var btnOpenMaps: Button

    // 我們用一個固定的主題名稱來做訂閱
    private val DANGER_TOPIC = "danger_alerts"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switchNotifications = findViewById(R.id.switchNotifications)
        btnOpenMaps = findViewById(R.id.btnOpenMaps)

        // 這裡我們不再用 Token，而是改用「主題訂閱」，這樣更靈活！
        // 檢查開關的狀態
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 如果開關被打開，就訂閱主題
                Firebase.messaging.subscribeToTopic(DANGER_TOPIC)
                    .addOnCompleteListener { task ->
                        var msg = "已開啟警報通知"
                        if (!task.isSuccessful) {
                            msg = "開啟通知失敗"
                        }
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    }
            } else {
                // 如果開關被關閉，就取消訂閱
                Firebase.messaging.unsubscribeFromTopic(DANGER_TOPIC)
                    .addOnCompleteListener { task ->
                        var msg = "已關閉警報通知"
                        if (!task.isSuccessful) {
                            msg = "關閉通知失敗"
                        }
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // 設定地圖按鈕的點擊事件
        btnOpenMaps.setOnClickListener {
            // 建立一個 Intent 來打開 Google Maps，不指定目的地，讓使用者自己決定
            val gmmIntentUri = Uri.parse("geo:0,0?q=")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                Toast.makeText(this, "請先安裝 Google Maps", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
package com.example.webweb

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // 載入我們簡化過的佈局

        // 設定一個延時任務，在 3 秒後執行
        Handler(Looper.getMainLooper()).postDelayed({
            // 建立一個要跳轉到 MapActivity 的意圖 (Intent)
            val intent = Intent(this, MapActivity::class.java)
            // 開始跳轉
            startActivity(intent)
            // 關閉目前的 MainActivity，這樣使用者按返回鍵時不會再回到這個啟動頁
            finish()
        }, 1500) // 延遲 3000 毫秒 (3秒)
    }
}
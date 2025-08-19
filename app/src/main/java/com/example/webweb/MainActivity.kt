package com.example.webweb

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var btnOpenMaps: Button
    private lateinit var btnStartStopGPS: Button // 新增按鈕

    private val DANGER_TOPIC = "danger_alerts"

    // 位置服務相關
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isTrackingLocation = false // 追蹤 GPS 狀態

    // HTTP 客戶端
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val FLASK_BACKEND_URL = "http://192.168.196.207:5000/receive_gps_data" // **重要：模擬器請用 10.0.2.2 訪問本地電腦**
    // **真機請用電腦的內網 IP，例如 http://192.168.1.xxx:5000**
    private val SESSION_ID = "test_session_mobile_001" // **用於測試的固定會話 ID，實際應動態生成**

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switchNotifications = findViewById(R.id.switchNotifications)
        btnOpenMaps = findViewById(R.id.btnOpenMaps)
        btnStartStopGPS = findViewById(R.id.btnStartStopGPS) // 初始化按鈕

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 設定 GPS 位置回調
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d(TAG, "Location received: Lat=${location.latitude}, Lng=${location.longitude}")
                    // 將位置數據發送到 Flask 後端
                    sendLocationDataToBackend(location)
                }
            }
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            // ... FCM 訂閱邏輯 (保持不變) ...
            if (isChecked) {
                Firebase.messaging.subscribeToTopic(DANGER_TOPIC)
                    .addOnCompleteListener { task ->
                        var msg = "已開啟警報通知"
                        if (!task.isSuccessful) { msg = "開啟通知失敗" }
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    }
            } else {
                Firebase.messaging.unsubscribeFromTopic(DANGER_TOPIC)
                    .addOnCompleteListener { task ->
                        var msg = "已關閉警報通知"
                        if (!task.isSuccessful) { msg = "關閉通知失敗" }
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    }
            }
        }

        btnOpenMaps.setOnClickListener {
            // ... 開啟地圖導航邏輯 (保持不變) ...
            val gmmIntentUri = Uri.parse("geo:0,0?q=")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                Toast.makeText(this, "請先安裝 Google Maps", Toast.LENGTH_SHORT).show()
            }
        }

        // 新增 GPS 追蹤按鈕點擊事件
        btnStartStopGPS.setOnClickListener {
            if (isTrackingLocation) {
                stopLocationUpdates()
            } else {
                startLocationUpdates()
            }
        }
    }

    // 啟動位置更新
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 請求位置權限
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        val locationRequest = LocationRequest.Builder(10000L) // 每 10 秒更新一次
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) // 高精度模式
            .setMinUpdateIntervalMillis(5000L) // 最快每 5 秒更新一次
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        isTrackingLocation = true
        btnStartStopGPS.text = "停止 GPS 追蹤"
        Toast.makeText(this, "開始 GPS 追蹤...", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Started GPS tracking.")
    }

    // 停止位置更新
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTrackingLocation = false
        btnStartStopGPS.text = "開始 GPS 追蹤"
        Toast.makeText(this, "停止 GPS 追蹤", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Stopped GPS tracking.")
    }

    // 處理權限請求結果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 權限已授予，再次嘗試啟動位置更新
                startLocationUpdates()
            } else {
                Toast.makeText(this, "位置權限被拒絕，無法追蹤 GPS。", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Location permission denied.")
            }
        }
    }

    // 發送位置數據到 Flask 後端
    private fun sendLocationDataToBackend(location: Location) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US) // ISO 8601 格式
        val timestamp = dateFormat.format(Date(location.time))

        val gpsData = mapOf(
            "session_id" to SESSION_ID,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to timestamp,
            "accuracy" to location.accuracy // 包含精確度
        )
        val json = Gson().toJson(gpsData)
        Log.d(TAG, "Sending GPS data: $json")

        CoroutineScope(Dispatchers.IO).launch { // 在 IO 協程中執行網路請求
            try {
                val request = Request.Builder()
                    .url(FLASK_BACKEND_URL)
                    .post(json.toRequestBody(JSON))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "GPS data sent successfully. Response: ${response.body?.string()}")
                } else {
                    Log.e(TAG, "Failed to send GPS data. Response: ${response.body?.string()}")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error sending GPS data: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error sending GPS data: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates() // 應用程式銷毀時停止追蹤
    }
}
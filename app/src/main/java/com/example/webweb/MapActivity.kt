package com.example.webweb

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: LatLng? = null
    private lateinit var editTextDestination: EditText
    private lateinit var btnPlanRoute: Button

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getCurrentLocationAndMoveCamera()
            } else {
                Toast.makeText(this, "需要定位權限才能規劃路線", Toast.LENGTH_LONG).show()
                val taipei101 = LatLng(25.033964, 121.564468)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(taipei101, 15f))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        editTextDestination = findViewById(R.id.editTextDestination)
        btnPlanRoute = findViewById(R.id.btnPlanRoute)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 設定按鈕的點擊事件，呼叫我們全新的、簡化的 planRoute 函式
        btnPlanRoute.setOnClickListener {
            planRoute()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkLocationPermission()
    }

    // ★★★ 這就是我們改回來的、超級簡單的 planRoute 函式 ★★★
    private fun planRoute() {
        val destination = editTextDestination.text.toString()
        if (destination.isBlank()) {
            Toast.makeText(this, "請輸入終點地址", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentLocation == null) {
            Toast.makeText(this, "正在獲取目前位置，請稍候...", Toast.LENGTH_SHORT).show()
            return
        }

        // 組合出啟動 Google Maps 導航的 URI
        val gmmIntentUri = Uri.parse("https://maps.google.com/maps?saddr=${currentLocation!!.latitude},${currentLocation!!.longitude}&daddr=$destination")

        // 建立一個 Intent 來呼叫外部 App
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps") // 直接指定用 Google Maps App 開啟

        // 檢查手機上是否有安裝 Google Maps App
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "請先安裝 Google Maps", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationPermission() {
        // ... 這個函式完全不變 ...
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocationAndMoveCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getCurrentLocationAndMoveCamera() {
        // ... 這個函式完全不變 ...
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        this.currentLocation = currentLatLng
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    } else {
                        Toast.makeText(this, "無法獲取目前位置，請確保 GPS 已開啟", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
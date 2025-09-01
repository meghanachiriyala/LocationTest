package com.example.locationtest

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var captureBtn: Button
    private lateinit var locationTv: TextView
    private lateinit var previewIv: ImageView

    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            LocationTestAPP.repository.startLocationUpdates()
            observeLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera launcher (returns a Bitmap preview)
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            // Add timestamp overlay
            val stampedBitmap = addTimestampToBitmap(bitmap)

            // Show in ImageView
            previewIv.setImageBitmap(stampedBitmap)
            Toast.makeText(this, "Photo captured", Toast.LENGTH_SHORT).show()

            // Save securely via repository
            lifecycleScope.launch {
                val saved = LocationTestAPP.repository.saveSecurePhoto(this@MainActivity, stampedBitmap)
                if (saved) {
                    Toast.makeText(this@MainActivity, "Photo saved securely", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to save photo", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Failed to capture photo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        captureBtn = findViewById(R.id.captureBtn)
        locationTv = findViewById(R.id.locationTv)
        previewIv = findViewById(R.id.previewIv)

        // Request permissions
        checkAndRequestPermissions()

        // Capture button
        captureBtn.setOnClickListener {
            takeSecurePhoto()
        }

        observeLocation()
    }

    private fun checkAndRequestPermissions() {
        // Location
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            LocationTestAPP.repository.startLocationUpdates()
        }

        // Camera
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun observeLocation() {
        LocationTestAPP.repository.latestLocation { location ->
            runOnUiThread {
                locationTv.text = if (location != null) {
                    "Lat: ${location.latitude}, Lon: ${location.longitude}"
                } else {
                    "No location yet"
                }
            }
        }
    }

    private fun takeSecurePhoto() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        takePictureLauncher.launch(null)
    }

    // Helper to add timestamp overlay
    private fun addTimestampToBitmap(original: Bitmap): Bitmap {
        val mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            isAntiAlias = true
            setShadowLayer(6f, 3f, 3f, Color.BLACK)
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = sdf.format(Date())

        val textBounds = Rect()
        paint.getTextBounds(timestamp, 0, timestamp.length, textBounds)

// Center coordinates
        val x = (mutableBitmap.width - textBounds.width()) / 2f
        val y = (mutableBitmap.height + textBounds.height()) / 2f

// Draw semi-transparent black background behind text
        val bgPaint = Paint().apply {
            color = Color.argb(150, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            x - 20,
            y - textBounds.height() - 20,
            x + textBounds.width() + 20,
            y + 20,
            bgPaint
        )

// Draw text
        canvas.drawText(timestamp, x, y, paint)

        return mutableBitmap

    }


}

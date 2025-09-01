package com.example.locationtest


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class Repository(private val ctx: Context) {

    private val fused: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(ctx)

    private var locationCallback: ((Location?) -> Unit)? = null


    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L // 10s
        ).build()

        fused.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                locationCallback?.invoke(result.lastLocation)
            }
        }, Looper.getMainLooper())
    }


    fun latestLocation(callback: (Location?) -> Unit) {
        locationCallback = callback

        if (ActivityCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            callback(null)
            return
        }

        fused.lastLocation
            .addOnSuccessListener { callback(it) }
            .addOnFailureListener { callback(null) }
    }

    suspend fun captureAndSaveSecurePhoto(context: Context): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                // Make dummy black bitmap
                val bmp = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.BLACK)

                val paint = Paint().apply {
                    color = Color.WHITE
                    textSize = 32f
                    isAntiAlias = true
                }

                val ts = System.currentTimeMillis()
                var locText = "No Location"

                // Try last location
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val task = fused.lastLocation
                    val loc = task.result
                    if (loc != null) {
                        locText = "Lat: ${loc.latitude}, Lon: ${loc.longitude}"
                    }
                }

                canvas.drawText(locText, 20f, 50f, paint)
                canvas.drawText("Time: $ts", 20f, 100f, paint)

                // Save file
                val dir = File(context.filesDir, "encrypted")
                dir.mkdirs()
                val file = File(dir, "photo_$ts.jpg")
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                // Return preview
                bmp
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    suspend fun saveSecurePhoto(ctx: Context, bitmap: Bitmap): Boolean {
        return try {
            val file = File(ctx.filesDir, "secure_photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}

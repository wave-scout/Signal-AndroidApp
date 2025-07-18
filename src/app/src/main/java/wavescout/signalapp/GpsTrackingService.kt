package wavescout.signalapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GpsTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()

        // Initialize the location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Create the notification channel (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "gps_channel_id",
                "GPS Tracking Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Build the persistent notification
        val notification = NotificationCompat.Builder(this, "gps_channel_id")
            .setContentTitle("WaveScout GPS")
            .setContentText("Sending GPS location to server")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your own icon
            .build()

        // Start foreground service
        startForeground(1, notification)

        // Start tracking loop
        startGpsLoop()

        return START_STICKY
    }

    /**
     * Main loop for getting GPS updates and sending to server
     */
    private fun startGpsLoop() {
        CoroutineScope(Dispatchers.IO).launch {
            var lastLat = 0.0
            var lastLon = 0.0
            var lastSentTime = System.currentTimeMillis()
            while (true) {
                val location = GetGpsPosition(applicationContext, fusedLocationClient)
                val now = System.currentTimeMillis()
                if (location.error == "") {
                    val changed = location.latitude != lastLat || location.longitude != lastLon
                    val thirtySecondsPassed = now - lastSentTime >= 30_000

                    // Only send if location has changed
                    broadcastLocationUpdate("Lat: ${location.latitude}, Lon: ${location.longitude}")
                    if (changed || thirtySecondsPassed) {
                        lastLat = location.latitude
                        lastLon = location.longitude
                        lastSentTime = now
                        // Send to server
                        sendLocationToServer(applicationContext, location.latitude, location.longitude)
                        val text = "Lat: ${location.latitude}, Lon: ${location.longitude}"
                        broadcastLocationUpdate(text)
                    }
                    else
                    {
                        val text = "Lat: ${location.latitude}, Lon: ${location.longitude} No change!"
                        broadcastLocationUpdate(text)
                    }




                } else {
                    // Broadcast the error message
                    broadcastLocationUpdate("Error: ${location.error}")
                }

                delay(3_000L) // Wait 30 seconds
            }
        }
    }

    /**
     * Broadcast location string to any listening UI components
     */
    private fun broadcastLocationUpdate(text: String) {
       var tada ="Service context: $this";

        val intent = Intent("GPS_UPDATE")
        intent.setPackage(packageName)
        intent.putExtra("location_text", text)
        Log.d("GpsTrackingService", "Broadcasting: $text")
        sendBroadcast(intent)
        Log.d("Broadcast", "sent")
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not used, service is not bound
        return null
    }
}

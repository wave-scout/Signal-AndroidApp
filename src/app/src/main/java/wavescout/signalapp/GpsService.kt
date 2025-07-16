package wavescout.signalapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.tasks.await

 suspend fun GetGpsPosition(context: Context, fusedLocationClient: FusedLocationProviderClient):GpsPosition {
       var result=GpsPosition();


        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                // Get last known location
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude

                    return GpsPosition(latitude = lat, longitude = lon, "")


                } else {
                   result = GpsPosition(error = "Location not available")
                }
            } catch (e: Exception) {
                result = GpsPosition(error = "Fel: ${e.message}")

            }
        } else {
            result = GpsPosition(error = "No authorization")
        }

        return result
    }

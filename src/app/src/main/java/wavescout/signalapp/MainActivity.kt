package wavescout.signalapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import wavescout.signalapp.datastore.TokenManager
import wavescout.signalapp.ui.theme.SignalAppTheme
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
class MainActivity : ComponentActivity() {

    private val LOCATION_PERMISSION_REQUEST = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if we have the authorization
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }

        setContent {
            SignalAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GPSLocationDisplay(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    @Composable
    fun GPSLocationDisplay(modifier: Modifier = Modifier) {

        val context = LocalContext.current
        var locationText by remember { mutableStateOf("Getting location...") }
        val token by TokenManager.getTokenFlow(context).collectAsState(initial = "")

        LaunchedEffect(token) {
            if (token=="") {
                TokenManager.saveToken(
                    context,
                    ""
                )
            }
        }


        LaunchedEffect(Unit) {

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            // Run forever as long as the composable is active
            while (true) {
                val gpsPosition = GetGpsPosition(context, fusedLocationClient)
                if (gpsPosition.error != "") {
                    locationText = gpsPosition.error;

                } else {
                    locationText = "Lat: ${gpsPosition.latitude}, Lon: ${gpsPosition.longitude}"
                    // Send to server
                    var serverResponse =
                        sendLocationToServer(token, gpsPosition.latitude, gpsPosition.longitude)
                    if (serverResponse != "") {
                        locationText = serverResponse
                    }
                }

                // wait 30 000 ms (30 s)
                kotlinx.coroutines.delay(3_000L)
            }
        }

        ShowLocationText(locationText, modifier)
    }

    @Composable
    fun ShowLocationText(text: String, modifier: Modifier = Modifier) {
        Text(text = text, modifier = modifier)
    }


    fun getSignalKAccess(): String? {
        return try {
            val loginUrl = URL("http://192.168.86.31:3000/signalk/v1/access/requests")
            val connection = loginUrl.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Skicka login-data som JSON
            val json = """
            {
              "clientId": "WaveScoutSignalClient",
              "description": "Wave scout signal client"
            }
        """.trimIndent()

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(json)
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val stream = connection.inputStream.bufferedReader().readText()
                val gson = Gson()
                val loginResponse = gson.fromJson(stream, AccessResponse::class.java)
                loginResponse.requestId
            } else {
                println("Login failed: HTTP $responseCode")
                null
            }

        } catch (e: Exception) {
            println("Login error: ${e.message}")
            null
        }
    }

    fun getSignalKToken(): String? {
        return try {
            var id = getSignalKAccess()

            val loginUrl = URL("http://192.168.86.31:3000/signalk/v1/requests/" + id)
            val connection = loginUrl.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true


            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val stream = connection.inputStream.bufferedReader().readText()
                val gson = Gson()
                val loginResponse = gson.fromJson(stream, AccessResponse::class.java)
                loginResponse.requestId
            } else {
                println("Login failed: HTTP $responseCode")
                null
            }

        } catch (e: Exception) {
            println("Login error: ${e.message}")
            null
        }
    }

}
suspend fun saveSignalRToken(context: Context, signalRToken: String) {
    val key = stringPreferencesKey("signalRToken")
    context.dataStore.edit { settings ->
        settings[key] = signalRToken
    }
}
data class AccessResponse(
    @SerializedName("state") val state: String,
    @SerializedName("requestId") val requestId: String,
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("href") val href: String,
    @SerializedName("ip") val ip: String,
)

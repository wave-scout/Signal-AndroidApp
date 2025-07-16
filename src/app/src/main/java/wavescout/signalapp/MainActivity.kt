package wavescout.signalapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
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
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import okhttp3.internal.wait
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

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
            val navController = rememberNavController()
            NavHost(navController, startDestination = "main") {
                composable("main") { MainScreen(navController) }
                composable("settings") { SettingsScreen(navController) }
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavHostController) {
    SignalAppTheme {
        Scaffold(
            containerColor = Color.Black
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // GPS display in the center or background
                GPSLocationDisplay(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp)
                )

                // Gear wheel top right
                IconButton(
                    onClick = { navController.navigate("settings") },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun GPSLocationDisplay(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    var locationText by remember { mutableStateOf("Click to start sync gps") }
    var isTracking by remember { mutableStateOf(false) }
    val sharedPref = context.getSharedPreferences("WSSignalSettings", Context.MODE_PRIVATE)
    var signalKToken = sharedPref.getString("signalK_token", "") ?: ""
    if (signalKToken == "") {
        //get a token
        // save token
        with(sharedPref.edit()) {
            putString("signalK_token", "token here")
            apply()
        }
    }



    LaunchedEffect(isTracking) {
        if (!isTracking) return@LaunchedEffect
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        var oldLat = 0.0
        var oldLong = 0.0
        // Run forever as long as the composable is active
        while (true) {
            val gpsPosition = GetGpsPosition(context, fusedLocationClient)
            if (gpsPosition.error != "") {
                locationText = gpsPosition.error;

            } else {
                locationText = "Lat: ${gpsPosition.latitude}, Lon: ${gpsPosition.longitude}"
                if (oldLat != gpsPosition.latitude || oldLong != gpsPosition.longitude) {
                    oldLat = gpsPosition.latitude
                    oldLong = gpsPosition.longitude
                    // Send to server
                    var serverResponse =
                        sendLocationToServer(context, gpsPosition.latitude, gpsPosition.longitude)
                    if (serverResponse != "") {
                        locationText = serverResponse
                    }
                } else {
                    locationText =
                        "Lat: ${gpsPosition.latitude}, Lon: ${gpsPosition.longitude} : No change!"
                }

            }

            // wait 30 000 ms (30 s)
            kotlinx.coroutines.delay(3_000L)
        }
    }
    // Show the text and a start button
    Column(modifier = modifier.padding(16.dp)) {
        Text(text = locationText, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { isTracking = true }) {
            Text("Start gps sync")
        }
    }

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


suspend fun saveSignalRToken(context: Context, signalRToken: String) {
    val key = stringPreferencesKey("signalRToken")
    context.dataStore.edit { settings ->
        settings[key] = signalRToken
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()
    MainScreen(navController = navController)
}

data class AccessResponse(
    @SerializedName("state") val state: String,
    @SerializedName("requestId") val requestId: String,
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("href") val href: String,
    @SerializedName("ip") val ip: String,
)

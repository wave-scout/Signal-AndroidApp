package wavescout.signalapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.google.gson.annotations.SerializedName
import wavescout.signalapp.ui.theme.SignalAppTheme
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
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
            val version="1.0.2"
            NavHost(navController, startDestination = "main") {
                composable("main") { MainScreen(version,navController) }
                composable("settings") { SettingsScreen(version, navController) }
            }
        }
    }
}

@Composable
fun MainScreen(version: String,navController: NavHostController) {
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
                GPSLocationDisplay(version,
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
fun GPSLocationDisplay(version: String,modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var locationText by remember { mutableStateOf("Click to start sync gps") }

    // Read saved Signal K token
    val sharedPref = context.getSharedPreferences("WSSignalSettings", Context.MODE_PRIVATE)
    val signalKToken = sharedPref.getString("signalK_token", "") ?: ""



    // Register BroadcastReceiver to listen for GPS updates from service
    DisposableEffect(Unit) {
        Log.d("GPSReceiver", "Registering receiver")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val message = intent?.getStringExtra("location_text")
                Log.d("GPSReceiver", "Received broadcast with text: $message")
                if (message != null) {
                    locationText = message
                }
            }
        }

        val filter = IntentFilter("GPS_UPDATE")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // UI layout
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = locationText, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                // Start the GPS tracking service
                val intent = Intent(context, GpsTrackingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }) {
                Text("Start gps sync")
            }
        }

        // App version in bottom-right corner
        Text(
            text = "v$version",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()
    MainScreen("preview",navController = navController)
}

data class AccessResponse(
    @SerializedName("state") val state: String,
    @SerializedName("requestId") val requestId: String,
    @SerializedName("accessRequest") val accessRequest: AccessRequest?,
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("href") val href: String,
    @SerializedName("ip") val ip: String,
)
data class AccessRequest(
    @SerializedName("permission") val permission: String,
    @SerializedName("token") val token: String
)

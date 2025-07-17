package wavescout.signalapp

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch

//@Preview(showBackground = true)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPref = context.getSharedPreferences("WSSignalSettings", Context.MODE_PRIVATE)
    val ip = sharedPref.getString("signalK_ip", "") ?: "192.168.x.x"
    val port = sharedPref.getString("signalK_port", "") ?: "xxxx"
    val signalKToken = sharedPref.getString("signalK_token", "No signal-K token!") ?: "No signal-K token!"
    var SignalKipAddress by remember { mutableStateOf(ip) }
    var SignalKPort by remember { mutableStateOf(port) }
    var tokenStatus by remember { mutableStateOf(signalKToken) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Tillbaka-knapp uppe till vänster
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        var statusText by remember { mutableStateOf("-") }
        // Centralt innehåll
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SETTINGS",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Signal k IP",
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = SignalKipAddress,
                onValueChange = { SignalKipAddress = it },
                label = { Text("192.168.x.x") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Signal k Port(default: 3000)",
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = SignalKPort,
                onValueChange = { SignalKPort = it },
                label = { Text("xxxx") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {

                val sharedPref = context.getSharedPreferences("WSSignalSettings", Context.MODE_PRIVATE)
                keyboardController?.hide()
                with (sharedPref.edit()) {
                    putString("signalK_ip", SignalKipAddress)
                    putString("signalK_port", SignalKPort)
                    apply() // eller commit() om du vill vänta på svar direkt
                }
                statusText="Updated!"

            }) {
                Text("Update")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusText,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tokenStatus,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                coroutineScope.launch {
                    val resultAccessRequest = SendSignalKAccessRequest(context)
                    if(resultAccessRequest=="")
                    {
                        statusText="Requested!"
                    }
                    else
                    {
                        statusText=resultAccessRequest.toString()
                    }
                }



            }) {
                Text("Send request")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                coroutineScope.launch {
                    val resultAccessRequest = CheckSignalKAccessRequest(context)
                    statusText=resultAccessRequest.toString()
                }
            })
            {
                Text(text = "Check access token")
            }
        }
        // Versionsnummer längst ner till höger
        Text(
            text = "v.1.0.0",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreen() {
    val navController = rememberNavController()
    SettingsScreen(navController = navController)
}

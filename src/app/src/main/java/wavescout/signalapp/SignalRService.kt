package wavescout.signalapp

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener


fun sendPositionOverWebSocket(context:Context, jsonString: String): String {

    val client = OkHttpClient()
    val sharedPref = context.getSharedPreferences("WSSignalSettings", Context.MODE_PRIVATE)
    val ip = sharedPref.getString("signalK_ip", "") ?: "192.168.x.x"
    val port = sharedPref.getString("signalK_port", "") ?: "xxxx"
    val signalKToken = sharedPref.getString("signalK_token", "") ?: ""
var url="ws://$ip:$port/signalk/v1/stream"
    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $signalKToken")
        .build()

    val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            println("WebSocket öppnad")
            val b = webSocket.send(jsonString)
            println("Skickade JSON:\n$jsonString")

            // Stäng efter skickat (valfritt)
            webSocket.close(1000, "Klart")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("❌ WebSocket failure: ${t.message}")
            println("📡 Response code: ${response?.code}")
            println("📄 Body: ${response?.body?.string()}")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            println("WebSocket stängd: $code – $reason")
        }
    }

    client.newWebSocket(request, listener)

    // Viktigt! Stänger inte klienten, så att den kan skicka färdigt
    client.dispatcher.executorService.shutdown()
    return ""
}


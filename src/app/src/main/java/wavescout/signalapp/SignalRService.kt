package wavescout.signalapp

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


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
suspend fun SendSignalKAccessRequest(context: Context): String? = withContext(Dispatchers.IO){
     try {
        val sharedPref = context.getSharedPreferences("WSSignalSettings", Context.MODE_PRIVATE)
        val ip = sharedPref.getString("signalK_ip", "") ?: "192.168.x.x"
        val port = sharedPref.getString("signalK_port", "") ?: "xxxx"
        val loginUrl = URL("http://$ip:$port/signalk/v1/access/requests")
        val connection = loginUrl.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
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

        if (responseCode == HttpURLConnection.HTTP_OK || responseCode==HttpURLConnection.HTTP_ACCEPTED) {
            val stream = connection.inputStream.bufferedReader().readText()
            val gson = Gson()
            val loginResponse = gson.fromJson(stream, AccessResponse::class.java)

            with (sharedPref.edit()) {
                putString("signalK_requestId", loginResponse.requestId)
                apply()
            }
            return@withContext ""

        } else {
            println("Access request failed: HTTP $responseCode")
            return@withContext "Access request failed: HTTP $responseCode"

        }

    } catch (e: Exception) {
        println("Access request error: ${e.message}")
        return@withContext "Access request error: ${e.message}"
    }

}
suspend fun CheckSignalKAccessRequest(context: Context): String? = withContext(Dispatchers.IO){

    //check token
    try {
        val sharedPref = context.getSharedPreferences("WSSignalSettings", Context.MODE_PRIVATE)
        val ip = sharedPref.getString("signalK_ip", "") ?: "192.168.x.x"
        val port = sharedPref.getString("signalK_port", "") ?: "xxxx"
       val id=sharedPref.getString("signalK_requestId", "") ?: "xxxx"

        val loginUrl = URL("http://$ip:$port/signalk/v1/requests/" + id)
        val connection = loginUrl.openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json")



        val responseCode = connection.responseCode

        if (responseCode == HttpURLConnection.HTTP_OK) {
            val stream = connection.inputStream.bufferedReader().readText()
            val gson = Gson()
            val loginResponse = gson.fromJson(stream, AccessResponse::class.java)
if(loginResponse.state=="PENDING")
{
    return@withContext "Access request is pending"
}
            else
{
    if(loginResponse.state=="COMPLETED")
    {
        if(loginResponse.accessRequest!=null)
        {
            with (sharedPref.edit()) {
                putString("signalK_token", loginResponse.accessRequest.token)
                apply()
            }
        }
    }

        return@withContext "Access request is "+loginResponse.state


}
            //return status
            //if token save and update text
        } else {
            return@withContext "Login failed: HTTP $responseCode"

        }

    } catch (e: Exception) {
        return@withContext "Login error: ${e.message}"

    }

    return@withContext ""
}

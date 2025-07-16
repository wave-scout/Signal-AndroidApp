package wavescout.signalapp
import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


suspend fun sendLocationToServer(context: Context, latitude: Double, longitude: Double):String {
    return withContext(Dispatchers.IO) {

        val updateData = SignalKUpdate(
            context = "vessels.self",
            updates = listOf(
                Update(
                    source = Source(label = "WaveScout-android-app"),
                    values = listOf(
                        Value(
                            path = "navigation.position",
                            value = LatLon(
                                latitude = latitude,
                                longitude = longitude
                            )
                        )
                    )
                )
            )
        )
        val gson = Gson()
        val json = gson.toJson(updateData)
        sendPositionOverWebSocket(context,  json)
    }
}
package wavescout.signalapp
data class SignalKUpdate(
    val context: String = "vessels.self",
    val updates: List<Update>
)

data class Update(
    val source: Source,
    val values: List<Value>
)

data class Source(
    val label: String
)

data class Value(
    val path: String,
    val value: LatLon
)

data class LatLon(
    val latitude: Double,
    val longitude: Double
)
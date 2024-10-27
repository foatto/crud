package foatto.server.ds

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

//--- outer JSON-class, don't rename fields!
@Serializable
data class PulsarData(
    val dateTime: Instant? = null,

    val deviceID: String? = null,
    val blockID: String? = null,

    val idx: Int? = null,
    val vals: List<Map<String, Double>>? = null,
)


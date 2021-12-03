package foatto.mms.core_mms.ds

import java.time.Instant

//--- JSON-class, do not rename fields!
class PulsarData(
    val dateTime: Instant? = null,

    val deviceID: String? = null,
    val blockID: String? = null,

    val idx: Int? = null,
    val vals: Array<Map<String,Double>>? = null,
)


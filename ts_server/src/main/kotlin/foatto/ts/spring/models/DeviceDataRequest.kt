package foatto.ts.spring.models

import kotlinx.serialization.Serializable

@Serializable
class DeviceDataRequest(
    val token: String,

    val type: String,
    val serialNo: String,

    val date: String? = null,
)
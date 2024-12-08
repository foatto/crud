package foatto.ts.spring.models

import kotlinx.serialization.Serializable

@Serializable
class DevicesDetailResponse(
    val companyId: Int,
    val companyName: String,

    val uds110: MutableMap<String, DeviceSensorShortData?>,
    val uds101: MutableMap<String, DeviceSensorShortData?>,
    val skk311: MutableMap<String, DeviceSensorShortData?>,
)

package foatto.ts.spring.models

import kotlinx.serialization.Serializable

@Serializable
class DeviceDataResponse(
    var companyId: Int,
    var companyName: String,

    var type: String,
    var serialNo: String,
    var vers: String,
    var imei: String,

    val rows: MutableList<DeviceSensorFullData>,
)
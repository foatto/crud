package foatto.ts.spring.models

import kotlinx.serialization.Serializable

@Serializable
class DeviceSensorShortData(
    val onTime: String,
    val depth: Double?,
    val speed: Double?,
    val force: Double?,
)
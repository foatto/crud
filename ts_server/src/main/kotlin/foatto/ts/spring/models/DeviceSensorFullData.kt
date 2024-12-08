package foatto.ts.spring.models

import kotlinx.serialization.Serializable

@Serializable
class DeviceSensorFullData(
    val onTime: String,
    val depth: Double?,
    val speed: Double?,
    val force: Double?,

    val mdmRC: Int? = null,
    val mdmCSQ: Int? = null,
    val mdmMany: Int? = null,

    val depthMin: Int? = null,
    val depthMax: Int? = null,
    val depthFlags: Int? = null,

    val speedMin: Int? = null,
    val speedMax: Int? = null,
    val speedFlags: Int? = null,

    val forceMin: Int? = null,
    val forceMax: Int? = null,
    val forceFlags: Int? = null,

    val wire: Int? = null,
    val encoder: Int? = null,
    val roller: Int? = null,

    val area: String? = null,
    val bush: String? = null,
    val well: String? = null,
    val shop: String? = null,
    val crew: String? = null,
    val auto: String? = null,
    val people: String? = null,
)

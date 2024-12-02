package foatto.ts.spring.models

import kotlinx.serialization.Serializable

@Serializable
class DevicesResponse(
    val companyId: Int,
    val companyName: String,

    val uds110: MutableList<Int>,
    val uds101: MutableList<Int>,
    val skk311: MutableList<Int>,
)


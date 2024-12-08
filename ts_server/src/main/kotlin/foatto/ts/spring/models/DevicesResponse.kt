package foatto.ts.spring.models

import kotlinx.serialization.Serializable

@Serializable
class DevicesResponse(
    val companyId: Int,
    val companyName: String,

    val uds110: MutableList<String>,
    val uds101: MutableList<String>,
    val skk311: MutableList<String>,
)
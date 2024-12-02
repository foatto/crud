package foatto.ts.spring.models

import kotlinx.serialization.Serializable

@Serializable
class DevicesRequest(

    val token: String,

    val companyId: Int? = null,
    val type: String? = null,
)
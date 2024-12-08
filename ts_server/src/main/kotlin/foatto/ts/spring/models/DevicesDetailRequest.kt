package foatto.ts.spring.models

import kotlinx.serialization.Serializable

@Serializable
class DevicesDetailRequest(
    val token: String,

    val type: String? = null,
    val companyId: Int? = null,
)
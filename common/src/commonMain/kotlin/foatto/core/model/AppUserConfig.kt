package foatto.core.model

import kotlinx.serialization.Serializable

@Serializable
class AppUserConfig(
    val currentUserName: String,
    val timeOffset: Int,
)
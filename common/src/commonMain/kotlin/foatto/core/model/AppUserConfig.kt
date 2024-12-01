package foatto.core.model

import kotlinx.serialization.Serializable

@Serializable
class AppUserConfig(
    val currentUserName: String,
    val isAdmin: Boolean,
    val timeOffset: Int,
    val userProperties: MutableMap<String, String>
)
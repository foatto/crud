package foatto.core.model

import kotlinx.serialization.Serializable

@Serializable
class AppModuleUrl(
    val appUrl: String,
    val formActionUrl: String,
)
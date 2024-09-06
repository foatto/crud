package foatto.core.model

import kotlinx.serialization.Serializable

@Serializable
class AppModuleUrl(
    val appUrl: String,
    val formActionUrl: String? = null,
    val chartActionUrl: String? = null,
    val mapActionUrl: String? = null,
    val schemeActionUrl: String? = null,
)
package foatto.core.model.response.xy.scheme

import foatto.core.model.response.ServerActionButton
import foatto.core.model.response.xy.XyElementConfig
import kotlinx.serialization.Serializable

@Serializable
class SchemeResponse(
    val elementConfigs: Map<String, XyElementConfig>,

    val tabCaption: String,
    val headers: List<String>,

    val alServerActionButton: List<ServerActionButton>? = null,
)
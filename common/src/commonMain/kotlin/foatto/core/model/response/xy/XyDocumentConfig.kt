package foatto.core.model.response.xy

import kotlinx.serialization.Serializable

@Serializable
class XyDocumentConfig(
    val name: String,
    val descr: String,
    val serverClassName: String,
    val isScaleAlign: Boolean,
    val hmElementConfig: Map<String, XyElementConfig>,
)

package foatto.core.model.response.xy

import kotlinx.serialization.Serializable

@Serializable
class XyElementConfig(
    val name: String,
    val clientType: XyElementClientType,
    val layer: Int,
    val scaleMin: Int,
    val scaleMax: Int,
    val descrForAction: String,
    val isRotatable: Boolean,
    val isMoveable: Boolean,
//    val isCopyable: Boolean,
    val isEditablePoint: Boolean
//    val isEditableText: Boolean,
)

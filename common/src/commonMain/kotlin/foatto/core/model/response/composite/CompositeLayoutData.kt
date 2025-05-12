package foatto.core.model.response.composite

import kotlinx.serialization.Serializable

@Serializable
class CompositeLayoutData(
    val isHidden: Boolean,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
)
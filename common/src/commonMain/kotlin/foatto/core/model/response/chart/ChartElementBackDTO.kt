package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartElementBackDTO(
    val x1: Int,
    val x2: Int,
    val color: Int,
)

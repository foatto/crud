package foatto.core.model.response.chart

import foatto.core.model.response.xy.geom.XyPoint
import kotlinx.serialization.Serializable

@Serializable
class ChartElementLineDTO(
    val x: Int,
    var y: Float,
    var colorIndex: ChartColorIndex,
    val coord: XyPoint? = null
)

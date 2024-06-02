package foatto.core.app.chart

import foatto.core.app.xy.geom.XyPoint
import kotlinx.serialization.Serializable

@Serializable
class ChartElementLineDTO(
    val x: Int,
    var y: Float,
    var colorIndex: ChartColorIndex,
    val coord: XyPoint? = null
)

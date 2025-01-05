package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartElementLine(
    val x: Int,
    var y: Float,
    var colorIndex: ChartColorIndex,
//    val coord: XyPoint? = null
)

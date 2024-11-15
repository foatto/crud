package foatto.compose.control.model.chart

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

sealed class ChartDrawData(
    val tooltip: String,
)

class ChartCircleDrawData(
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val strokeColor: Color? = null,
    val fillColor: Color? = null,
    tooltip: String = "",
) : ChartDrawData(tooltip)

class ChartLineDrawData(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val strokeColor: Color,
    val strokeWidth: Float,
    val strokeDash: FloatArray? = null,
    tooltip: String = "",
) : ChartDrawData(tooltip)

class ChartRectDrawData(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val strokeColor: Color? = null,
    val strokeWidth: Float = 0.0f,
    val fillColor: Color? = null,
//    val rx: Number = 0,
//    val ry: Number = 0,
    tooltip: String = ""
) : ChartDrawData(tooltip)

class ChartTextDrawData(
    val x: Float,
    val y: Float,
    val textLimitWidth: Float? = null,
    val textLimitHeight: Float? = null,
    var textAnchor: Alignment = Alignment.TopStart, // may be reassigned later
    val rotateDegree: Float? = null,
    val text: String,
    val fillColor: Color? = null,
    val strokeColor: Color? = null,
    val strokeWidth: Float? = null,
    val textColor: Color,
    tooltip: String = ""
) : ChartDrawData(tooltip)


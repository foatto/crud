package foatto.core_web

const val MIN_USER_RECT_SIZE = 8

abstract class SvgElement(val tooltip: String)

class SvgCircle(
    val cx: Int,
    val cy: Int,
    val radius: Int,
    val stroke: String = "",
    val fill: String = "",
    tooltip: String = ""
) : SvgElement(tooltip)

class SvgLine(
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    val stroke: String,
    val width: Int,
    val dash: String = "",
    tooltip: String = ""
) : SvgElement(tooltip)

//<rect x="0" y="0" width="200" height="100" fill="#BBC42A" rx="5" ry="10"/>
class SvgRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val stroke: String = "none",
    val fill: String = "none",
    val strokeWidth: Int = 0,
    val rx: String = "",    // м.б. в виде 0.1rem или типа того
    val ry: String = "",
    tooltip: String = ""
) : SvgElement(tooltip)

class SvgText(
    val x: Int,
    val y: Int,
    val text: String,
    val stroke: String,
    val hAnchor: String,
    val vAnchor: String,
    val transform: String = "",
    tooltip: String = ""
) : SvgElement(tooltip)

class SvgMultiLineText(
    val x: Int,
    val y: Int,
    val arrText: Array<SVGTextSpan>,
    val stroke: String,
    var hAnchor: String,
    val vAnchor: String,
    tooltip: String = ""
) : SvgElement(tooltip)

class SVGTextSpan(
    val dy: String,
    val text: String
)

//--- пусть будет общим для Graphic & Xy ------------------------------------------------------------

class MouseRectData(
    var isVisible: Boolean,
    var x1: Int,
    var y1: Int,
    var x2: Int,
    var y2: Int,
    var lineWidth: Int
)


package foatto.core_compose_web.control

import androidx.compose.runtime.MutableState

const val MIN_USER_RECT_SIZE = 8

abstract class SvgElementData(val tooltip: String)

class SvgCircleData(
    val cx: Int,
    val cy: Int,
    val radius: Int,
    val stroke: String = "",
    val fill: String = "",
    tooltip: String = ""
) : SvgElementData(tooltip)

class SvgLineData(
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    val stroke: String,
    val width: Int,
    val dash: String = "",
    tooltip: String = ""
) : SvgElementData(tooltip)

class SvgRectData(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val stroke: String = "none",
    val fill: String = "none",
    val strokeWidth: Int = 0,
    val rx: Number = 0,    // в чистом SVG м.б. в виде 0.1rem или типа того, но в Compose это абстрактный Number без единицы измерения, видимо, просто .px
    val ry: Number = 0,
    tooltip: String = ""
) : SvgElementData(tooltip)

class SvgTextData(
    val x: Int,
    val y: Int,
    val text: String,
    val stroke: String,
    var hAnchor: String,    // may be reassigned later
    val vAnchor: String,
    val transform: String = "",
    tooltip: String = ""
) : SvgElementData(tooltip)

//class SvgMultiLineText(
//    val x: Int,
//    val y: Int,
//    val arrText: Array<SVGTextSpan>,
//    val stroke: String,
//    var hAnchor: String,
//    val vAnchor: String,
//    tooltip: String = ""
//) : SvgElement(tooltip)

//class SVGTextSpan(
//    val dy: String,
//    val text: String
//)

//--- пусть будет общим для Graphic & Xy ------------------------------------------------------------

class MouseRectData(
    val isVisible: MutableState<Boolean>,
    val x1: MutableState<Int>,
    val y1: MutableState<Int>,
    val x2: MutableState<Int>,
    val y2: MutableState<Int>,
    val lineWidth: MutableState<Int>,
)


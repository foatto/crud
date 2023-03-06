package foatto.core_compose_web.util

import foatto.core_compose_web.style.COLOR_TRANSPARENT
import org.jetbrains.compose.web.css.CSSColorValue

const val MIN_USER_RECT_SIZE = 8

abstract class SvgElementData(val tooltip: String)

class SvgCircleData(
    val cx: Int,
    val cy: Int,
    val radius: Int,
    val stroke: CSSColorValue = COLOR_TRANSPARENT,
    val fill: String = "",
    tooltip: String = ""
) : SvgElementData(tooltip)

class SvgLineData(
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    val stroke: CSSColorValue,
    val width: Int,
    val dash: String = "",
    tooltip: String = ""
) : SvgElementData(tooltip)

class SvgRectData(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val stroke: CSSColorValue = COLOR_TRANSPARENT,
    val strokeWidth: Int = 0,
    val fill: String = "none",
    val rx: Number = 0,    // в чистом SVG м.б. в виде 0.1rem или типа того, но в Compose это абстрактный Number без единицы измерения, видимо, просто .px
    val ry: Number = 0,
    tooltip: String = ""
) : SvgElementData(tooltip)

class SvgTextData(
    val x: Int,
    val y: Int,
    val text: String,
    val stroke: CSSColorValue,
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


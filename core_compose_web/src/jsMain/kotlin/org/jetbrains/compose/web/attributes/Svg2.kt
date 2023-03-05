package org.jetbrains.compose.web.attributes

import org.jetbrains.compose.web.css.CSSColorValue
import org.w3c.dom.svg.*

//--- Circle attributes

fun AttrsScope<SVGCircleElement>.stroke(stroke: CSSColorValue): AttrsScope<SVGCircleElement> =
    attr("stroke", stroke.toString())

fun AttrsScope<SVGCircleElement>.strokeWidth(width: Int): AttrsScope<SVGCircleElement> =
    attr("stroke-width", width.toString())

fun AttrsScope<SVGCircleElement>.strokeDasharray(dasharray: String): AttrsScope<SVGCircleElement> =
    attr("stroke-dasharray", dasharray)

//--- Ellipse attributes

fun AttrsScope<SVGEllipseElement>.stroke(stroke: CSSColorValue): AttrsScope<SVGEllipseElement> =
    attr("stroke", stroke.toString())

fun AttrsScope<SVGEllipseElement>.strokeWidth(width: Int): AttrsScope<SVGEllipseElement> =
    attr("stroke-width", width.toString())

fun AttrsScope<SVGEllipseElement>.strokeDasharray(dasharray: String): AttrsScope<SVGEllipseElement> =
    attr("stroke-dasharray", dasharray)

//--- Line attributes

fun AttrsScope<SVGLineElement>.stroke(stroke: CSSColorValue): AttrsScope<SVGLineElement> =
    attr("stroke", stroke.toString())

fun AttrsScope<SVGLineElement>.strokeWidth(width: Int): AttrsScope<SVGLineElement> =
    attr("stroke-width", width.toString())

fun AttrsScope<SVGLineElement>.strokeDasharray(dasharray: String): AttrsScope<SVGLineElement> =
    attr("stroke-dasharray", dasharray)

//--- Path attributes

fun AttrsScope<SVGPathElement>.stroke(stroke: CSSColorValue): AttrsScope<SVGPathElement> =
    attr("stroke", stroke.toString())

fun AttrsScope<SVGPathElement>.strokeWidth(width: Int): AttrsScope<SVGPathElement> =
    attr("stroke-width", width.toString())

fun AttrsScope<SVGPathElement>.strokeDasharray(dasharray: String): AttrsScope<SVGPathElement> =
    attr("stroke-dasharray", dasharray)

//--- Polygon attributes

fun AttrsScope<SVGPolygonElement>.stroke(stroke: CSSColorValue): AttrsScope<SVGPolygonElement> =
    attr("stroke", stroke.toString())

fun AttrsScope<SVGPolygonElement>.strokeWidth(width: Int): AttrsScope<SVGPolygonElement> =
    attr("stroke-width", width.toString())

fun AttrsScope<SVGPolygonElement>.strokeDasharray(dasharray: String): AttrsScope<SVGPolygonElement> =
    attr("stroke-dasharray", dasharray)

//--- Polyline attributes

fun AttrsScope<SVGPolylineElement>.stroke(stroke: CSSColorValue): AttrsScope<SVGPolylineElement> =
    attr("stroke", stroke.toString())

fun AttrsScope<SVGPolylineElement>.strokeWidth(width: Int): AttrsScope<SVGPolylineElement> =
    attr("stroke-width", width.toString())

fun AttrsScope<SVGPolylineElement>.strokeDasharray(dasharray: String): AttrsScope<SVGPolylineElement> =
    attr("stroke-dasharray", dasharray)

//--- Rect attributes

fun AttrsScope<SVGRectElement>.stroke(stroke: CSSColorValue): AttrsScope<SVGRectElement> =
    attr("stroke", stroke.toString())

fun AttrsScope<SVGRectElement>.strokeWidth(width: Int): AttrsScope<SVGRectElement> =
    attr("stroke-width", width.toString())

fun AttrsScope<SVGRectElement>.strokeDasharray(dasharray: String): AttrsScope<SVGRectElement> =
    attr("stroke-dasharray", dasharray)

//--- Text attributes

fun AttrsScope<SVGTextElement>.textAnchor(anchor: String): AttrsScope<SVGTextElement> =
    attr("text-anchor", anchor)

fun AttrsScope<SVGTextElement>.dominantBaseline(baseline: String): AttrsScope<SVGTextElement> =
    attr("dominant-baseline", baseline)


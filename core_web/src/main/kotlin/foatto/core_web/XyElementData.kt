package foatto.core_web

import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyElement
import foatto.core.app.xy.XyViewCoord
import foatto.core.app.xy.geom.XyLine
import foatto.core.app.xy.geom.XyPoint
import foatto.core.app.xy.geom.XyPolygon
import foatto.core.app.xy.geom.XyRect
import foatto.core.link.XyElementClientType
import foatto.core.link.XyElementConfig
import kotlin.js.Json
import kotlin.js.json
import kotlin.math.max
import kotlin.math.roundToInt

enum class XyElementDataType {
    CIRCLE,
    ELLIPSE,
    IMAGE,
    LINE,
    PATH,
    POLYLINE,
    POLYGON,
    RECT,
    TEXT
}

enum class AddPointStatus { COMPLETED, COMPLETEABLE, NOT_COMPLETEABLE }

class XyElementData(
    val type: XyElementDataType,

    var x: Int? = null,
    var y: Int? = null,

    val x1: Int? = null,
    val y1: Int? = null,
    var x2: Int? = null,
    var y2: Int? = null,

    var points: String? = null,

//    val textStyle: String? = null,

    val width: Int? = null,
    val height: Int? = null,

    val radius: Int? = null,

    val rx: Int? = 0,
    val ry: Int? = 0,

    val transform: String? = null,

    val strokeWidth: Int? = null,
    val strokeDash: String = "",

    val stroke: String? = null,
    val fill: String? = null,

//    val hAlign: String? = null,
//    val vAlign: String? = null,

    val url: String? = null,

    val tooltip: String? = null,

    var itReadOnly: Boolean = true,

    //--- для текстов (рисуемых через div)
    var isVisible: Boolean = false,
    var text: String? = null,
    var pos: Json = json(),
    val style: Json = json(),

    //--- поэлементный интерактив
    //--- пока заполняем только для ZONE, т.к. для других элементов пока нет интерактива

    var itSelected: Boolean = false,

    val alPoint: MutableList<XyPoint>? = null,

    val itEditablePoint: Boolean = false,
    val itMoveable: Boolean = false,

    val typeName: String? = null,
    val alAddInfo: List<Pair<String,() -> String>>? = null,
    val elementID: Int? = null

) {

//    fun isContains( /*scaleKoef: Int,*/ aX: Int, aY: Int ): Boolean {
//        when( type ) {
//            XyElementDataType.CIRCLE -> {
//                return XyPoint.distance( aX.toDouble(), aY.toDouble(), x!!.toDouble(), y!!.toDouble() ) <= radius!!.toDouble()
//            }
//            XyElementDataType.ELLIPSE -> {
//                //--- пока не буду заморачиваться (нет интерактивных элементов-эллипсов), сделаю почти как у круга
//                return XyPoint.distance( aX.toDouble(), aY.toDouble(), x!!.toDouble(), y!!.toDouble() ) <= (( rx!! + ry!! ) / 2 ).toDouble()
//            }
//            XyElementDataType.IMAGE,
//            XyElementDataType.RECT -> {
//                return XyRect( x!!, y!!, width!!, height!! ).isContains( aX, aY )
//            }
//            XyElementDataType.LINE -> {
//                return XyLine.distanceSeg( x1!!.toDouble(), y1!!.toDouble(), x2!!.toDouble(), y2!!.toDouble(), aX.toDouble(), aY.toDouble() ) <= 1  //scaleKoef
//            }
//            XyElementDataType.PATH,
//            XyElementDataType.POLYLINE,
//            XyElementDataType.POLYGON -> {
//                if( alPoint != null ) {
//                    for( i in 0 until alPoint.size ) {
//                        val p1 = alPoint[ i ]
//                        val p2 = alPoint[ i + 1 ]
//                        if( XyLine.distanceSeg( p1.x.toDouble(), p1.y.toDouble(), p2.x.toDouble(), p2.y.toDouble(), aX.toDouble(), aY.toDouble() ) <= 1 )   //scaleKoef )
//                            return true
//                    }
//                    if( type == XyElementDataType.POLYGON ) {
//                        val p1 = alPoint.first()
//                        val p2 = alPoint.last()
//                        if( XyLine.distanceSeg( p1.x.toDouble(), p1.y.toDouble(), p2.x.toDouble(), p2.y.toDouble(), aX.toDouble(), aY.toDouble() ) <= 1 )   //scaleKoef )
//                            return true
//                    }
//                }
//                return false
//            }
//            XyElementDataType.TEXT -> {
//                return false
//            }
//        }
//    }

    fun isIntersects( rect: XyRect): Boolean {
        when( type ) {
            XyElementDataType.CIRCLE -> {
                return rect.isIntersects( XyRect( x!! - radius!!, y!! - radius, radius * 2, radius * 2 ) )
            }
            XyElementDataType.ELLIPSE -> {
                return rect.isIntersects( XyRect( x!! - rx!! , y!! - ry!!, rx * 2, ry * 2 ) )
            }
            XyElementDataType.IMAGE,
            XyElementDataType.RECT -> {
                return rect.isIntersects( XyRect( x!!, y!!, width!!, height!! ) )
            }
            XyElementDataType.LINE -> {
                return rect.isIntersects( XyLine( x1!!, y1!!, x2!!, y2!! ) )
            }
            XyElementDataType.PATH,
            XyElementDataType.POLYLINE,
            XyElementDataType.POLYGON -> {
                if( alPoint != null && alPoint.size >= 3 ) {
                    //--- заполненный полигон полностью снаружи прямоугольника
                    if( type == XyElementDataType.POLYGON && fill!!.isNotEmpty()) {
                        val poly = XyPolygon( alPoint )
                        if( poly.isContains( rect.x,              rect.y ) ) return true
                        if( poly.isContains( rect.x + rect.width, rect.y ) ) return true
                        if( poly.isContains( rect.x,              rect.y + rect.height ) ) return true
                        if( poly.isContains( rect.x + rect.width, rect.y + rect.height ) ) return true
                    }
                    //--- фигура полностью внутри прямоугольника
                    alPoint.forEach {
                        if( rect.isContains( it ) )
                            return true
                    }
                    //--- фигура пересекается с краями прямоугольника
                    for( i in 0..( alPoint.size - 2 ) ) {
                        val p1 = alPoint[ i ]
                        val p2 = alPoint[ i + 1 ]
                        if( rect.isIntersects( XyLine( p1, p2 ) ) )
                            return true
                    }
                    if( type == XyElementDataType.POLYGON ) {
                        val p1 = alPoint.first()
                        val p2 = alPoint.last()
                        if( rect.isIntersects( XyLine( p1, p2 ) ) )
                            return true
                    }
                }
                return false
            }
            XyElementDataType.TEXT -> {
                return false
            }
        }
    }

    fun setLastPoint( mouseX: Int, mouseY: Int ) {
        when( type ) {
            XyElementDataType.POLYGON -> {
                if( alPoint!!.isNotEmpty() ) {
                    alPoint.last().set( mouseX, mouseY )
                    points = ""
                    alPoint.forEach {
                        points += "${it.x},${it.y} "
                    }
                }
            }
        }
    }

    //--- применяется только в ADD_POINT
    fun addPoint( mouseX: Int, mouseY: Int ): AddPointStatus {
        when( type ) {
            XyElementDataType.POLYGON -> {
                //--- при первом клике при добавлении полигона добавляем сразу две точки - первая настоящая, вторая/последняя - служебная
                if( alPoint!!.isEmpty() )
                    alPoint.add( XyPoint( mouseX, mouseY ) )
                alPoint.add( XyPoint( mouseX, mouseY ) )
                points = ""
                alPoint.forEach {
                    points += "${it.x},${it.y} "
                }
                //--- последняя точка служебная, в полигон не войдёт
                return if( alPoint.size > 3 ) AddPointStatus.COMPLETEABLE else AddPointStatus.NOT_COMPLETEABLE
            }
        }
        return AddPointStatus.NOT_COMPLETEABLE
    }

    //--- применяется только в EDIT_POINT
    fun insertPoint( index: Int, mouseX: Int, mouseY: Int ) {
        when( type ) {
            XyElementDataType.POLYGON -> {
                alPoint!!.add( index, XyPoint( mouseX, mouseY ) )
                points = ""
                alPoint.forEach {
                    points += "${it.x},${it.y} "
                }
            }
        }
    }

    //--- применяется только в EDIT_POINT
    fun setPoint( index: Int, mouseX: Int, mouseY: Int ) {
        when( type ) {
            XyElementDataType.POLYGON -> {
                alPoint!![ index ] = XyPoint( mouseX, mouseY )
                points = ""
                alPoint.forEach {
                    points += "${it.x},${it.y} "
                }
            }
        }
    }

    //--- применяется только в EDIT_POINT
    fun removePoint( index: Int ) {
        when( type ) {
            XyElementDataType.POLYGON -> {
                alPoint!!.removeAt( index )
                points = ""
                alPoint.forEach {
                    points += "${it.x},${it.y} "
                }
            }
        }
    }

    //--- применяется только в Move
    fun moveRel( dx: Int, dy: Int ) {
        when( type ) {
            XyElementDataType.POLYGON -> {
                points = ""
                alPoint!!.forEach {
                    it.x += dx
                    it.y += dy
                    points += "${it.x},${it.y} "
                }
            }
        }
    }

    fun doAddElement( that: dynamic, documentTypeName: String, startParamID: String, scaleKoef: Double, viewCoord: XyViewCoord) {
        lateinit var xyActionRequest: XyActionRequest

        when( type ) {
            XyElementDataType.POLYGON -> {
                val xyElement = XyElement(
                    typeName = typeName!!,
                    elementID = 0,
                    objectID = 0
                )
                //--- переводим в мировые координаты
                xyElement.alPoint = alPoint!!.map {
                    XyPoint( viewCoord.x1 + ( it.x * viewCoord.scale / scaleKoef ).roundToInt(),
                             viewCoord.y1 + ( it.y * viewCoord.scale / scaleKoef ).roundToInt() )
                }.dropLast( 1 ).toTypedArray()  // убираем последнюю служебную точку

                xyActionRequest = XyActionRequest(
                    documentTypeName = documentTypeName,
                    action = XyAction.ADD_ELEMENT,
                    startParamID = startParamID,
                    xyElement = xyElement
                )

                alAddInfo!!.forEach {
                    xyActionRequest.hmParam[ it.first ] = it.second()
                }
            }
//            //--- приводим в локальные/экранные размеры
//            actionElement!!.element.imageWidth *= xyModel.viewCoord.scale / scaleKoef
//            actionElement!!.element.imageHeight *= xyModel.viewCoord.scale / scaleKoef
        }

        that.`$root`.setWait( true )
        invokeXy(
            xyActionRequest,
            {
                that.`$root`.setWait( false )
                that.refreshView( that, null )
            }
        )
    }

    fun doEditElementPoint( that: dynamic, documentTypeName: String, startParamID: String, scaleKoef: Double, viewCoord: XyViewCoord ) {
        lateinit var xyActionRequest: XyActionRequest

        when( type ) {
            XyElementDataType.POLYGON -> {
                val xyElement = XyElement(
                    typeName = "",      // неважно для редактирования точек
                    elementID = elementID!!,
                    objectID = 0
                )
                //--- переводим в мировые координаты
                xyElement.alPoint = alPoint!!.map {
                    XyPoint( viewCoord.x1 + ( it.x * viewCoord.scale / scaleKoef ).roundToInt(),
                             viewCoord.y1 + ( it.y * viewCoord.scale / scaleKoef ).roundToInt() )
                }.toTypedArray()

                xyActionRequest = XyActionRequest(
                    documentTypeName = documentTypeName,
                    action = XyAction.EDIT_ELEMENT_POINT,
                    startParamID = startParamID,
                    xyElement = xyElement
                )
            }
//            //--- приводим в локальные/экранные размеры
//            actionElement!!.element.imageWidth *= xyModel.viewCoord.scale / scaleKoef
//            actionElement!!.element.imageHeight *= xyModel.viewCoord.scale / scaleKoef
        }

        that.`$root`.setWait( true )
        invokeXy(
            xyActionRequest,
            {
                that.`$root`.setWait( false )
                that.refreshView( that, null )
            }
        )
    }

}

fun readXyElementData(scaleKoef: Double, viewCoord: XyViewCoord, elementConfig: XyElementConfig, element: XyElement, alLayer: MutableList<XyElementData> ) {

    val lineWidth = element.lineWidth
    val drawColor = if( element.drawColor == 0 ) "#00000000" else getColorFromInt( element.drawColor )
    val fillColor = if( element.fillColor == 0 ) "#00000000" else getColorFromInt( element.fillColor )

    when( elementConfig.clientType.toString() ) {
        XyElementClientType.BITMAP.toString() -> {
            val p = element.alPoint.first()

            if( element.imageName.isBlank() ) {
                alLayer.add( XyElementData(
                    type = XyElementDataType.RECT,
                    x = ( ( p.x - viewCoord.x1 ) / viewCoord.scale * scaleKoef ).roundToInt(),
                    y = ( ( p.y - viewCoord.y1 ) / viewCoord.scale * scaleKoef ).roundToInt(),
                    width = ( element.imageWidth / viewCoord.scale * scaleKoef ).roundToInt(),
                    height = ( element.imageHeight / viewCoord.scale * scaleKoef ).roundToInt(),
                    stroke = "gray",
                    strokeWidth = 1, //scaleKoef,
                    strokeDash = "${scaleKoef * 2},${scaleKoef * 2}",
                    tooltip = element.imageName,
                    itReadOnly = element.itReadOnly
                ) )
            }
            else {
                alLayer.add( XyElementData(
                    type = XyElementDataType.IMAGE,
                    x = ( ( p.x - viewCoord.x1 ) / viewCoord.scale * scaleKoef ).roundToInt(),
                    y = ( ( p.y - viewCoord.y1 ) / viewCoord.scale * scaleKoef ).roundToInt(),
                    width = ( element.imageWidth / viewCoord.scale * scaleKoef ).roundToInt(),
                    height = ( element.imageHeight / viewCoord.scale * scaleKoef ).roundToInt(),
                    url = element.imageName,
                    itReadOnly = element.itReadOnly
                ) )
            }
        }
        XyElementClientType.ICON.toString() -> {
            val p = element.alPoint.first()

            alLayer.add( XyElementData(
                type = XyElementDataType.IMAGE,
                x = ( ( ( p.x - viewCoord.x1 ) / viewCoord.scale - element.calcAnchorXKoef() * element.imageWidth ).toInt() * scaleKoef ).roundToInt(),
                y = ( ( ( p.y - viewCoord.y1 ) / viewCoord.scale - element.calcAnchorYKoef() * element.imageHeight ).toInt() * scaleKoef ).roundToInt(),
                width = ( element.imageWidth * scaleKoef ).roundToInt(),
                height = ( element.imageHeight * scaleKoef ).roundToInt(),
                transform = "rotate(${element.rotateDegree} ${( ( p.x - viewCoord.x1 ) / viewCoord.scale * scaleKoef ).roundToInt()} ${( ( p.y - viewCoord.y1 ) / viewCoord.scale * scaleKoef ).roundToInt()})",
                url = element.imageName,
                tooltip = element.toolTipText,
                itReadOnly = element.itReadOnly
            ) )
        }
        XyElementClientType.MARKER.toString() -> {
            //--- для ускорения отрисовки вытащим точки
            val p = element.alPoint.first()
            val x = ( ( p.x - viewCoord.x1 ) / viewCoord.scale * scaleKoef ).roundToInt()
            val y = ( ( p.y - viewCoord.y1 ) / viewCoord.scale * scaleKoef ).roundToInt()
            val halfX = ( element.markerSize * scaleKoef / 2 ).roundToInt()
            val halfY = ( element.markerSize * scaleKoef / 2 ).roundToInt()

            when( element.markerType.toString() ) {

                XyElement.MarkerType.ARROW.toString() -> {
                    val points =
                        "${ x - halfX / 2 },$y " +
                        "${ x - halfX },${ y - halfY } " +
                        "${ x + halfX * 2 },$y " +
                        "${ x - halfX },${ y + halfY } "
                    alLayer.add( XyElementData(
                        type = XyElementDataType.POLYGON,
                        points = points,
                        stroke = drawColor,
                        fill = fillColor,
                        strokeWidth = max( 1.0, lineWidth * scaleKoef ).roundToInt(),
                        strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                        transform = "rotate(${element.rotateDegree} $x $y)",
                        tooltip = element.toolTipText,
                        itReadOnly = element.itReadOnly
                    ) )
                }
                XyElement.MarkerType.CIRCLE.toString() -> {
                    if( element.markerSize2 == 0 || element.markerSize == element.markerSize2 ) {
                        alLayer.add( XyElementData(
                            type = XyElementDataType.CIRCLE,
                            x = x,
                            y = y,
                            radius = halfX,
                            stroke = drawColor,
                            fill = fillColor,
                            strokeWidth = max( 1.0, lineWidth * scaleKoef ).roundToInt(),
                            strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                            transform = "rotate(${element.rotateDegree} $x $y)",
                            tooltip = element.toolTipText,
                            itReadOnly = element.itReadOnly
                        ) )
                    }
                    else {
                        alLayer.add( XyElementData(
                            type = XyElementDataType.ELLIPSE,
                            x = x,
                            y = y,
                            rx = ( element.markerSize * scaleKoef / 2 ).roundToInt(),
                            ry = ( element.markerSize2 * scaleKoef / 2 ).roundToInt(),
                            stroke = drawColor,
                            fill = fillColor,
                            strokeWidth = max( 1.0, lineWidth * scaleKoef ).roundToInt(),
                            strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                            transform = "rotate(${element.rotateDegree} $x $y)",
                            tooltip = element.toolTipText,
                            itReadOnly = element.itReadOnly
                        ) )
                    }
                }
                XyElement.MarkerType.CROSS.toString() -> {
                    val path =
                        "M${ x - halfX },${ y - halfY }" +
                        "L${ x + halfX },${ y + halfY }" +
                        "M${ x + halfX },${ y - halfY }" +
                        "L${ x - halfX },${ y + halfY }"
                    alLayer.add( XyElementData(
                        type = XyElementDataType.PATH,
                        points = path,
                        stroke = drawColor,
                        fill = fillColor,
                        strokeWidth = max( 1.0, lineWidth * scaleKoef ).roundToInt(),
                        strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                        transform = "rotate(${element.rotateDegree} $x $y)",
                        tooltip = element.toolTipText,
                        itReadOnly = element.itReadOnly
                    ) )
                }
                XyElement.MarkerType.DIAMOND.toString() -> {
                    val points =
                        "$x,${ y - halfY } " +
                        "${ x + halfX },$y " +
                        "$x,${ y + halfY } " +
                        "${ x - halfX },$y "
                    alLayer.add( XyElementData(
                        type = XyElementDataType.POLYGON,
                        points = points,
                        stroke = drawColor,
                        fill = fillColor,
                        strokeWidth = max( 1.0, lineWidth * scaleKoef ).roundToInt(),
                        strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                        transform = "rotate(${element.rotateDegree} $x $y)",
                        tooltip = element.toolTipText,
                        itReadOnly = element.itReadOnly
                    ) )
                }
                XyElement.MarkerType.PLUS.toString() -> {
                    val path =
                        "M$x,${ y - halfY }" +
                        "L$x,${ y + halfY }" +
                        "M${ x - halfX },$y" +
                        "L${ x + halfX },$y"
                    alLayer.add( XyElementData(
                        type = XyElementDataType.PATH,
                        points = path,
                        stroke = drawColor,
                        fill = fillColor,
                        strokeWidth = max( 1.0, lineWidth * scaleKoef ).roundToInt(),
                        strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                        transform = "rotate(${element.rotateDegree} $x $y)",
                        tooltip = element.toolTipText,
                        itReadOnly = element.itReadOnly
                    ) )
                }
                XyElement.MarkerType.SQUARE.toString() -> {
                    alLayer.add( XyElementData(
                        type = XyElementDataType.RECT,
                        x = x - halfX,
                        y = y - halfY,
                        width = 2 * halfX,
                        height = 2 * halfY,
                        stroke = drawColor,
                        fill = fillColor,
                        strokeWidth = max( 1.0, lineWidth * scaleKoef ).roundToInt(),
                        strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                        transform = "rotate(${element.rotateDegree} $x $y)",
                        tooltip = element.toolTipText,
                        itReadOnly = element.itReadOnly
                    ) )
                }
                XyElement.MarkerType.TRIANGLE.toString() -> {
                    val points =
                        "$x,${ y - halfY } " +
                        "${ x + halfX },${ y + halfY } " +
                        "${ x - halfX },${ y + halfY } "
                    alLayer.add( XyElementData(
                        type = XyElementDataType.POLYGON,
                        points = points,
                        stroke = drawColor,
                        fill = fillColor,
                        strokeWidth = max( 1.0, lineWidth * scaleKoef ).roundToInt(),
                        strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                        transform = "rotate(${element.rotateDegree} $x $y)",
                        tooltip = element.toolTipText,
                        itReadOnly = element.itReadOnly
                    ) )
                }
            }
        }
        XyElementClientType.POLY.toString() -> {
            var points = ""
            element.alPoint.forEach {
                val x = ( ( it.x - viewCoord.x1 ) / viewCoord.scale * scaleKoef ).roundToInt()
                val y = ( ( it.y - viewCoord.y1 ) / viewCoord.scale * scaleKoef ).roundToInt()
                points += "$x,$y "
            }
            alLayer.add( XyElementData(
                type = if( element.itClosed ) XyElementDataType.POLYGON else XyElementDataType.POLYLINE,
                points = points,
                stroke = drawColor,
                fill = fillColor,
                strokeWidth = max( 1.0, lineWidth * scaleKoef ).roundToInt(),
                strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                tooltip = element.toolTipText,
                itReadOnly = element.itReadOnly
            ) )
        }
        XyElementClientType.TEXT.toString() -> {
            val p = element.alPoint.first()

            var x = ( ( p.x - viewCoord.x1 ) / viewCoord.scale * scaleKoef ).roundToInt()
            var y = ( ( p.y - viewCoord.y1 ) / viewCoord.scale * scaleKoef ).roundToInt()

            val limitWidth = ( element.limitWidth / viewCoord.scale * scaleKoef ).roundToInt()
            val limitHeight = ( element.limitHeight / viewCoord.scale * scaleKoef ).roundToInt()

            val textColor = if( element.textColor == 0 ) "#00000000" else getColorFromInt( element.textColor )

            var style = json(
                "position" to "absolute",
                "padding" to styleXyTextPadding(),
                "color" to textColor,
                "text-align" to when( element.alignX.toString() ) {
                        XyElement.Align.LT.toString() -> "left"
                        XyElement.Align.RB.toString() -> "right"
                        else -> "center"
                    },
                "vertical-align" to when( element.alignY.toString() ) {
                        XyElement.Align.LT.toString() -> "text-top"
                        XyElement.Align.RB.toString() -> "text-bottom"
                        else -> "baseline"
                    }
            )

            if( element.drawColor != 0 )
                style.add( json(
                    "border-radius" to "${2 * scaleKoef}px",
                    "border" to "${1 * scaleKoef}px solid $drawColor"
                ) )

            if( element.fillColor != 0 )
                style.add( json(
                    "background" to fillColor
                ) )


            if( limitWidth > 0 || limitHeight > 0 ) {
                style.add( json(
                    "overflow" to "hidden"
                ) )
            }
            if( limitWidth > 0 ) {
                val dx = if( element.anchorX.toString() == XyElement.Anchor.LT.toString() ) 0
                    else if( element.anchorX.toString() == XyElement.Anchor.CC.toString() ) limitWidth / 2
                    else /*anchorX == ANCHOR_RB ?*/ limitWidth

                x -= dx

                style.add( json(
                    "width" to "${limitWidth}px"   // чтобы избежать некрасивого перекрытия прямоугольников
                ) )
            }
            if( limitHeight > 0 ) {
                val dy = if( element.anchorY.toString() == XyElement.Anchor.LT.toString() ) 0
                    else if( element.anchorY.toString() == XyElement.Anchor.CC.toString() ) limitHeight / 2
                    else /*anchorY == ANCHOR_RB ?*/ limitHeight

                y -= dy

                style.add( json(
                    "height" to "${limitHeight}px"
                ) )
            }

            alLayer.add( XyElementData(
                type = XyElementDataType.TEXT,
                x = x,
                y = y,
                text = element.text.replace( "\n", "<br>" ),
                tooltip = element.toolTipText,
                itReadOnly = element.itReadOnly,

                isVisible = true,       //!!! д.б. false и потом отдельно включаться в зависимости от заэкранности положения
                pos = json(
                    "left" to "${x}px",
                    "top" to "${y}px"
                ),
                style = style
            ) )
        }
        XyElementClientType.TRACE.toString() -> {
            for( i in 0 until element.alPoint.size - 1 ) {
                val p1 = element.alPoint[ i ]
                val p2 = element.alPoint[ i + 1 ]

                alLayer.add( XyElementData(
                    type = XyElementDataType.LINE,
                    x1 = ( ( p1.x - viewCoord.x1 ) / viewCoord.scale * scaleKoef ).roundToInt(),
                    y1 = ( ( p1.y - viewCoord.y1 ) / viewCoord.scale * scaleKoef ).roundToInt(),
                    x2 = ( ( p2.x - viewCoord.x1 ) / viewCoord.scale * scaleKoef ).roundToInt(),
                    y2 = ( ( p2.y - viewCoord.y1 ) / viewCoord.scale * scaleKoef ).roundToInt(),
                    stroke = getColorFromInt( element.alDrawColor[ i ] ),
                    strokeWidth = max( 1.0, lineWidth * scaleKoef ).roundToInt(),
                    tooltip = element.alToolTip[ i ],
                    itReadOnly = element.itReadOnly
                ) )
            }
        }
        XyElementClientType.ZONE.toString() -> {
            var points = ""
            val alPoint = mutableListOf<XyPoint>()
            element.alPoint.forEach {
                val x = ( ( it.x - viewCoord.x1 ) / viewCoord.scale * scaleKoef ).roundToInt()
                val y = ( ( it.y - viewCoord.y1 ) / viewCoord.scale * scaleKoef ).roundToInt()

                points += "$x,$y "
                alPoint.add( XyPoint( x, y ) )
            }
            alLayer.add( XyElementData(
                type = XyElementDataType.POLYGON,
                points = points,
                stroke = COLOR_XY_ZONE_ACTUAL,
                fill = COLOR_XY_ZONE_ACTUAL,
                strokeWidth = ( 2 * scaleKoef ).roundToInt(),
                strokeDash = "${scaleKoef * 2 /*lineWidth*/ * 2},${scaleKoef * 2 /*lineWidth*/ * 2}",
                tooltip = element.toolTipText,
                itReadOnly = element.itReadOnly,
                alPoint = alPoint,
                itEditablePoint = !element.itReadOnly,
                itMoveable = !element.itReadOnly,
                elementID = element.elementID
            ) )
        }
    }
}

fun getXyEmptyElementData( scaleKoef: Double, elementConfig: XyElementConfig) : XyElementData? {
    var result: XyElementData? = null

    when( elementConfig.clientType.toString() ) {
        XyElementClientType.BITMAP.toString() -> {
        }
        XyElementClientType.ICON.toString() -> {
        }
        XyElementClientType.MARKER.toString() -> {
        }
        XyElementClientType.POLY.toString() -> {
        }
        XyElementClientType.TEXT.toString() -> {
        }
        XyElementClientType.TRACE.toString() -> {
        }
        XyElementClientType.ZONE.toString() -> {
            result = XyElementData(
                type = XyElementDataType.POLYGON,
                points = "",
                stroke = COLOR_XY_ZONE_ACTUAL,
                fill = COLOR_XY_ZONE_ACTUAL,
                strokeWidth = ( 2 * scaleKoef ).roundToInt(),
                strokeDash = "${scaleKoef * 2 /*lineWidth*/ * 2},${scaleKoef * 2 /*lineWidth*/ * 2}",
                tooltip = "",
                itReadOnly = false,
                alPoint = mutableListOf(),
                itEditablePoint = true,
                itMoveable = true,
                //--- при добавлении сразу выбранный
                itSelected = true,
                //--- данные для добавления на сервере
                typeName = "mms_zone",
                alAddInfo = listOf(
                    Pair( "zone_name", {
                        ( kotlinx.browser.window.prompt( "Введите наименование геозоны" )?.trim() ?: "" ).ifEmpty { "-" }
                    } ),
                    Pair( "zone_descr", {
                        ( kotlinx.browser.window.prompt( "Введите описание геозоны" )?.trim() ?: "" ).ifEmpty { "-" }
                    } )
                )
            )
        }
    }

    return result
}
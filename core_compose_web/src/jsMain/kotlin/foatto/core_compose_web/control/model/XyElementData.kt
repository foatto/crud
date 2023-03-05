package foatto.core_compose_web.control.model

import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyElement
import foatto.core.app.xy.XyViewCoord
import foatto.core.app.xy.geom.XyLine
import foatto.core.app.xy.geom.XyPoint
import foatto.core.app.xy.geom.XyPolygon
import foatto.core.app.xy.geom.XyRect
import foatto.core_compose_web.Root
import foatto.core_compose_web.control.AbstractXyControl
import foatto.core_compose_web.link.invokeXy
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.StyleScope
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
    SVG_TEXT,
    HTML_TEXT,
}

enum class AddPointStatus { COMPLETED, COMPLETEABLE, NOT_COMPLETEABLE }

class XyElementData(
    val type: XyElementDataType,
    val elementId: Int,
    val objectId: Int,

    var x: Int = 0,
    var y: Int = 0,

    val x1: Int = 0,
    val y1: Int = 0,
    var x2: Int = 0,
    var y2: Int = 0,

    //--- точки для SVG могут быть затребованы или строкой или одномерным массивом чисел (бардак в SVG, да)
    var strPoints: String? = null,
    var arrPoints: Array<out Number>? = null,

    val width: Int = 0,
    val height: Int = 0,

    val radius: Int = 0,

    val rx: Int = 0,
    val ry: Int = 0,

    val transform: String? = null,

    val stroke: CSSColorValue? = null,
    val strokeWidth: Int? = null,
    val strokeDash: String? = null,

    val fill: CSSColorValue? = null,

    //--- для SVG-текста
    val hAnchor: String = "middle",
    val vAnchor: String = "central",

    val url: String = "",

    val tooltip: String? = null,

    var itReadOnly: Boolean = true,

    //--- для HTML-текстов (рисуемых через div)
    var isVisible: Boolean = false,
    var text: String = "",
    var pos: StyleScope.() -> Unit = {},

    //--- для всех видов текста
    val style: StyleScope.() -> Unit = {},

    //--- поэлементный интерактив
    //--- пока заполняем только для ZONE, т.к. для других элементов пока нет интерактива

    var itSelected: Boolean = false,

    val alPoint: MutableList<XyPoint>? = null,

    val itEditablePoint: Boolean = false,
    val itMoveable: Boolean = false,

    val typeName: String? = null,
    val alAddInfo: List<Pair<String, () -> String>>? = null,

    val dialogQuestion: String? = null,
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

    fun isIntersects(rect: XyRect): Boolean {
        when (type) {
            XyElementDataType.CIRCLE -> {
                return rect.isIntersects(XyRect(x - radius, y - radius, radius * 2, radius * 2))
            }

            XyElementDataType.ELLIPSE -> {
                return rect.isIntersects(XyRect(x - rx, y - ry, rx * 2, ry * 2))
            }

            XyElementDataType.IMAGE,
            XyElementDataType.RECT -> {
                return rect.isIntersects(XyRect(x, y, width, height))
            }

            XyElementDataType.LINE -> {
                return rect.isIntersects(XyLine(x1, y1, x2, y2))
            }

            XyElementDataType.PATH,
            XyElementDataType.POLYLINE,
            XyElementDataType.POLYGON -> {
                if (alPoint != null && alPoint.size >= 3) {
                    //--- заполненный полигон полностью снаружи прямоугольника
                    if (type == XyElementDataType.POLYGON && fill != null) {
                        val poly = XyPolygon(alPoint)
                        if (poly.isContains(rect.x, rect.y)) {
                            return true
                        }
                        if (poly.isContains(rect.x + rect.width, rect.y)) {
                            return true
                        }
                        if (poly.isContains(rect.x, rect.y + rect.height)) {
                            return true
                        }
                        if (poly.isContains(rect.x + rect.width, rect.y + rect.height)) {
                            return true
                        }
                    }
                    //--- фигура полностью внутри прямоугольника
                    alPoint.forEach {
                        if (rect.isContains(it)) {
                            return true
                        }
                    }
                    //--- фигура пересекается с краями прямоугольника
                    for (i in 0..(alPoint.size - 2)) {
                        val p1 = alPoint[i]
                        val p2 = alPoint[i + 1]
                        if (rect.isIntersects(XyLine(p1, p2))) {
                            return true
                        }
                    }
                    if (type == XyElementDataType.POLYGON) {
                        val p1 = alPoint.first()
                        val p2 = alPoint.last()
                        if (rect.isIntersects(XyLine(p1, p2))) {
                            return true
                        }
                    }
                }
                return false
            }

            XyElementDataType.SVG_TEXT, XyElementDataType.HTML_TEXT -> {
                return false
            }
        }
    }

    fun setLastPoint(mouseX: Int, mouseY: Int) {
        when (type) {
            XyElementDataType.POLYGON -> {
                if (alPoint!!.isNotEmpty()) {
                    alPoint.last().set(mouseX, mouseY)
                    calcArrPoints()
                }
            }

            else -> {}
        }
    }

    //--- применяется только в ADD_POINT
    fun addPoint(mouseX: Int, mouseY: Int): AddPointStatus {
        when (type) {
            XyElementDataType.POLYGON -> {
                //--- при первом клике при добавлении полигона добавляем сразу две точки - первая настоящая, вторая/последняя - служебная
                if (alPoint!!.isEmpty()) {
                    alPoint.add(XyPoint(mouseX, mouseY))
                }
                alPoint.add(XyPoint(mouseX, mouseY))
                calcArrPoints()
                //--- последняя точка служебная, в полигон не войдёт
                return if (alPoint.size > 3) {
                    AddPointStatus.COMPLETEABLE
                } else {
                    AddPointStatus.NOT_COMPLETEABLE
                }
            }

            else -> {}
        }
        return AddPointStatus.NOT_COMPLETEABLE
    }

    //--- применяется только в EDIT_POINT
    fun insertPoint(index: Int, mouseX: Int, mouseY: Int) {
        when (type) {
            XyElementDataType.POLYGON -> {
                alPoint!!.add(index, XyPoint(mouseX, mouseY))
                calcArrPoints()
            }

            else -> {}
        }
    }

    //--- применяется только в EDIT_POINT
    fun setPoint(index: Int, mouseX: Int, mouseY: Int) {
        when (type) {
            XyElementDataType.POLYGON -> {
                alPoint!![index] = XyPoint(mouseX, mouseY)
                calcArrPoints()
            }

            else -> {}
        }
    }

    //--- применяется только в EDIT_POINT
    fun removePoint(index: Int) {
        when (type) {
            XyElementDataType.POLYGON -> {
                alPoint!!.removeAt(index)
                calcArrPoints()
            }

            else -> {}
        }
    }

    //--- применяется только в Move
    fun moveRel(dx: Int, dy: Int) {
        when (type) {
            XyElementDataType.POLYGON -> {
                alPoint!!.forEach {
                    it.x += dx
                    it.y += dy
                }
                calcArrPoints()
            }

            else -> {}
        }
    }

    fun doAddElement(
        root: Root,
        xyControl: AbstractXyControl,
        documentTypeName: String,
        startParamId: String,
        scaleKoef: Double,
        viewCoord: XyViewCoord
    ) {
        lateinit var xyActionRequest: XyActionRequest

        when (type) {
            XyElementDataType.POLYGON -> {
                val xyElement = XyElement(
                    typeName = typeName!!,
                    elementId = 0,
                    objectId = 0
                )
                //--- переводим в мировые координаты
                xyElement.alPoint = alPoint!!.map {
                    XyPoint(
                        viewCoord.x1 + (it.x * viewCoord.scale / scaleKoef).roundToInt(),
                        viewCoord.y1 + (it.y * viewCoord.scale / scaleKoef).roundToInt()
                    )
                }.dropLast(1).toTypedArray()  // убираем последнюю служебную точку

                xyActionRequest = XyActionRequest(
                    documentTypeName = documentTypeName,
                    action = XyAction.ADD_ELEMENT,
                    startParamId = startParamId,
                    xyElement = xyElement
                )

                alAddInfo!!.forEach {
                    xyActionRequest.hmParam[it.first] = it.second()
                }
            }

            else -> {}
//            //--- приводим в локальные/экранные размеры
//            actionElement!!.element.imageWidth *= xyModel.viewCoord.scale / scaleKoef
//            actionElement!!.element.imageHeight *= xyModel.viewCoord.scale / scaleKoef
        }

        root.setWait(true)
        invokeXy(xyActionRequest) {
            root.setWait(false)
            xyControl.xyRefreshView(null, true)
        }
    }

    fun doEditElementPoint(
        root: Root,
        xyControl: AbstractXyControl,
        documentTypeName: String,
        startParamId: String,
        scaleKoef: Double,
        viewCoord: XyViewCoord
    ) {
        lateinit var xyActionRequest: XyActionRequest

        when (type) {
            XyElementDataType.POLYGON -> {
                val xyElement = XyElement(
                    typeName = "",      // неважно для редактирования точек
                    elementId = elementId,
                    objectId = 0
                )
                //--- переводим в мировые координаты
                xyElement.alPoint = alPoint!!.map {
                    XyPoint(
                        viewCoord.x1 + (it.x * viewCoord.scale / scaleKoef).roundToInt(),
                        viewCoord.y1 + (it.y * viewCoord.scale / scaleKoef).roundToInt()
                    )
                }.toTypedArray()

                xyActionRequest = XyActionRequest(
                    documentTypeName = documentTypeName,
                    action = XyAction.EDIT_ELEMENT_POINT,
                    startParamId = startParamId,
                    xyElement = xyElement
                )
            }

            else -> {}
//            //--- приводим в локальные/экранные размеры
//            actionElement!!.element.imageWidth *= xyModel.viewCoord.scale / scaleKoef
//            actionElement!!.element.imageHeight *= xyModel.viewCoord.scale / scaleKoef
        }

        root.setWait(true)
        invokeXy(
            xyActionRequest
        ) {
            root.setWait(false)
            xyControl.xyRefreshView(null, true)
        }
    }

    private fun calcArrPoints() {
        val points = mutableListOf<Int>()
        alPoint?.forEach { xyPoint ->
            points += xyPoint.x
            points += xyPoint.y
        }
        arrPoints = points.toTypedArray()
    }
}

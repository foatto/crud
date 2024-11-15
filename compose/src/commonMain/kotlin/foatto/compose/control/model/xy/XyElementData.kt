package foatto.compose.control.model.xy

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import foatto.core.model.response.xy.geom.XyLine
import foatto.core.model.response.xy.geom.XyPoint
import foatto.core.model.response.xy.geom.XyPolygon
import foatto.core.model.response.xy.geom.XyRect

class XyElementData(
    val type: XyElementDataType,
    val elementId: Int,
    val objectId: Int,

//---------------------------------

    var x: Float = 0.0f,
    var y: Float = 0.0f,

    val x1: Float = 0.0f,
    val y1: Float = 0.0f,
    var x2: Float = 0.0f,
    var y2: Float = 0.0f,

    var alPoints: MutableList<XyPoint> = mutableListOf(),
    val isClosed: Boolean = false,

    val width: Float = 0.0f,
    val height: Float = 0.0f,

    val radius: Float = 0.0f,

    val startAngle: Float = 0.0f,
    val sweepAngle: Float = 0.0f,

    val rx: Float = 0.0f,
    val ry: Float = 0.0f,

    val rotateDegree: Float? = null,

//---------------------------------

    val strokeColor: Color? = null,
    val strokeWidth: Float? = null,
    val strokeDash: FloatArray? = null,

    val fillColor: Color? = null,

    val alpha: Float = 1.0f,

//---------------------------------

    val text: String = "",
    val textLimitWidth: Float? = null,
    val textLimitHeight: Float? = null,
    val textAnchor: Alignment = Alignment.TopStart,
    val textColor: Color? = null,
    val textFontSize: Int = 8,
    val textIsBold: Boolean = false,

//---------------------------------

    val url: String = "",

//---------------------------------

    //--- поэлементный интерактив
    //--- пока заполняем только для ZONE, т.к. для других элементов пока нет интерактива

    var isVisible: Boolean = true, //!!! не лучше ли mutableStateOf ?
    var isSelected: Boolean = false,

    var isReadOnly: Boolean = true,
    val isEditablePoint: Boolean = false,
    val isMoveable: Boolean = false,

    val typeName: String? = null,
    val alAddInfo: List<Pair<String, () -> String>>? = null,

    val dialogQuestion: String? = null,
) {

    fun getPath(): Path = Path().apply {
        val p0 = alPoints[0]
        moveTo(p0.x.toFloat(), p0.y.toFloat())
        for (i in 1..<alPoints.size) {
            val p = alPoints[i]
            lineTo(p.x.toFloat(), p.y.toFloat())
        }
        if (isClosed) {
            lineTo(p0.x.toFloat(), p0.y.toFloat())
        }
    }

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
            XyElementDataType.ARC -> {  //!!! временно: по упрощённой формуле, считая дугу как полный круг
                return rect.isIntersects(XyRect(x - radius, y - radius, radius * 2, radius * 2))
            }

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
                return rect.isIntersects(XyLine(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt()))
            }

            XyElementDataType.POLY -> {
                if (alPoints.size >= 3) {
                    //--- заполненный полигон полностью снаружи прямоугольника
                    if (isClosed) {
                        val poly = XyPolygon(alPoints)
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
                    alPoints.forEach {
                        if (rect.isContains(it)) {
                            return true
                        }
                    }
                    //--- фигура пересекается с краями прямоугольника
                    for (i in 0..(alPoints.size - 2)) {
                        val p1 = alPoints[i]
                        val p2 = alPoints[i + 1]
                        if (rect.isIntersects(XyLine(p1, p2))) {
                            return true
                        }
                    }
                    if (isClosed) {
                        val p1 = alPoints.first()
                        val p2 = alPoints.last()
                        if (rect.isIntersects(XyLine(p1, p2))) {
                            return true
                        }
                    }
                }
                return false
            }

            XyElementDataType.TEXT -> {
                return false
            }
        }
    }

    fun setLastPoint(mouseX: Int, mouseY: Int) {
        when (type) {
            XyElementDataType.POLY -> {
                if (alPoints.isNotEmpty()) {
                    alPoints.last().set(mouseX, mouseY)
                }
            }

            else -> {}
        }
    }

    //--- применяется только в ADD_POINT
    fun addPoint(mouseX: Int, mouseY: Int): AddPointStatus {
        when (type) {
            XyElementDataType.POLY -> {
                //--- при первом клике при добавлении полигона добавляем сразу две точки - первая настоящая, вторая/последняя - служебная
                if (alPoints!!.isEmpty()) {
                    alPoints.add(XyPoint(mouseX, mouseY))
                }
                alPoints.add(XyPoint(mouseX, mouseY))
                //--- последняя точка служебная, в полигон не войдёт
                return if (alPoints.size > 3) {
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
            XyElementDataType.POLY -> {
                alPoints.add(index, XyPoint(mouseX, mouseY))
            }

            else -> {}
        }
    }

    //--- применяется только в EDIT_POINT
    fun setPoint(index: Int, mouseX: Int, mouseY: Int) {
        when (type) {
            XyElementDataType.POLY -> {
                alPoints[index] = XyPoint(mouseX, mouseY)
            }

            else -> {}
        }
    }

    //--- применяется только в EDIT_POINT
    fun removePoint(index: Int) {
        when (type) {
            XyElementDataType.POLY -> {
                alPoints.removeAt(index)
            }

            else -> {}
        }
    }

    //--- применяется только в Move
    fun moveRel(dx: Int, dy: Int) {
        when (type) {
            XyElementDataType.POLY -> {
                alPoints.forEach { p ->
                    p.x += dx
                    p.y += dy
                }
            }

            else -> {}
        }
    }

}

package foatto.core.app.xy

import foatto.core.app.xy.geom.XyPoint

class XyElement(   // именно data class, понадобится .copy
    val typeName: String,
    var elementID: Int,
    var objectID: Int
) {

    companion object {
        //--- коэффициент дополнительного огрубления при генерализации:
        //--- для топографии будем огрублять до 1 pix видимого размера/промежутка
        val GEN_KOEF = 1
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    enum class MarkerType { ARROW, CIRCLE, CROSS, DIAMOND, PLUS, SQUARE, TRIANGLE }
    enum class Anchor { LT, CC, RB }
    enum class Align { LT, CC, RB }
    enum class ArrowPos { NONE, BEGIN, MIDDLE, END }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    var alPoint = arrayOf<XyPoint>()
    var itClosed = false

    var lineWidth = 0

    var drawColor = 0
    var fillColor = 0

    //--- позиция точки привязки относительно изображения или текста
    var anchorX = Anchor.CC
    var anchorY = Anchor.CC

    var rotateDegree = 0.0

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    var toolTipText: String = ""
    var itReadOnly: Boolean = false
    var itActual: Boolean = false

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- Bitmap
    var imageName = ""
    //--- для Bitmap: реальная/масштабируемая, для Icon: видимая/экранная ширина и высота изображения
    var imageWidth = 0
    var imageHeight = 0

    //--- Marker
    var markerType = MarkerType.CIRCLE
    var markerSize = 0
    var markerSize2 = 0

    //--- Text
    var text = ""
    var textColor = 0
    var fontSize = 12
    var itFontBold = false
    var itFontItalic = false

    //--- ограничения текста
    var limitWidth = 0
    var limitHeight = 0
    //--- выравнивание текст
    var alignX = Align.LT
    var alignY = Align.LT

    //--- Trace
    var arrowPos = ArrowPos.NONE
    var arrowLen = 0
    var arrowHeight = 0
    var arrowLineWidth = 0

    val alDrawColor = arrayOf<Int>()
    val alFillColor = arrayOf<Int>()
    val alToolTip = arrayOf<String>()

//--- внутренние методы ---------------------------------------------------------------------------------------------------------------------------------------------------------------

//    fun addTracePoint( aP: XyPoint, aColor: Int, aToolTip: String ) {
//        alPoint.add( aP )
//
//        alDrawColor.add( aColor )
//        alFillColor.add( aColor )
//        alToolTip.add( aToolTip )
//    }

    fun calcAnchorXKoef() =
        when( anchorX.toString() ) {
            Anchor.LT.toString() -> 0.0f
            Anchor.CC.toString() -> 0.5f
            Anchor.RB.toString() -> 1.0f
            else -> 0.5f
        }

    fun calcAnchorYKoef() =
        when( anchorY.toString() ) {
            Anchor.LT.toString() -> 0.0f
            Anchor.CC.toString() -> 0.5f
            Anchor.RB.toString() -> 1.0f
            else -> 0.5f
        }
}

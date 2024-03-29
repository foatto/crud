package foatto.core.app.xy

import foatto.core.app.iCoreAppContainer
import foatto.core.app.xy.geom.XyPoint
import kotlinx.serialization.Serializable

@Serializable
class XyElement(
    val typeName: String,
    var elementId: Int,
    var objectId: Int,
) {

    companion object {
        //--- coefficient of additional coarsening during generalization:
        //--- for topography we will coarse up to 1 pix of apparent size / interval
        val GEN_KOEF = 1
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    enum class MarkerType { ARROW, CIRCLE, CROSS, DIAMOND, PLUS, SQUARE, TRIANGLE }
    enum class Anchor { LT, CC, RB }
    enum class Align { LT, CC, RB }
    enum class ArrowPos { NONE, BEGIN, MIDDLE, END }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    var alPoint = listOf<XyPoint>()
    var isClosed = false

    var lineWidth = 0

    var drawColor = 0
    var fillColor = 0

    //--- the position of the anchor point relative to the image or text
    var anchorX = Anchor.CC
    var anchorY = Anchor.CC

    var rotateDegree = 0.0

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    var toolTipText: String = ""
    var isReadOnly: Boolean = false
    var isActual: Boolean = false

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- Bitmap
    var imageName = ""

    //--- for Bitmap: real / scalable, for Icon: visible / screen width and image height
    var imageWidth = 0
    var imageHeight = 0

    //--- Marker
    var markerType = MarkerType.CIRCLE
    var markerSize = 0
    var markerSize2 = 0

    //--- Text
    var text = ""
    var textColor = 0
    var fontSize = iCoreAppContainer.BASE_FONT_SIZE
    var isFontBold = false

    //--- text restrictions
    var limitWidth = 0
    var limitHeight = 0

    //--- text alignment
    var alignX = Align.LT
    var alignY = Align.LT

    //--- Trace
    var arrowPos = ArrowPos.NONE
    var arrowLen = 0
    var arrowHeight = 0
    var arrowLineWidth = 0

    var alDrawColor = listOf<Int>()
    var alFillColor = listOf<Int>()
    var alToolTip = listOf<String>()

    var dialogQuestion: String = ""

//------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun calcAnchorXKoef(): Float =
        when (anchorX) {
            Anchor.LT -> 0.0f
            Anchor.CC -> 0.5f
            Anchor.RB -> 1.0f
        }

    fun calcAnchorYKoef(): Float =
        when (anchorY) {
            Anchor.LT -> 0.0f
            Anchor.CC -> 0.5f
            Anchor.RB -> 1.0f
        }
}

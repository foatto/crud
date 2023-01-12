package foatto.core_compose_web.style

import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.properties.borderColor

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

typealias CSSSize = CSSSizeValue<out CSSUnit>

//--- Z-INDEX --------------------------------------------------------------------------------------------------------------------------------------------------

const val Z_INDEX_TABLE_CAPTION: Int = 1
const val Z_INDEX_TABLE_POPUP: Int = 2
const val Z_INDEX_GRAPHIC_VISIBILITY_LIST: Int = 10
const val Z_INDEX_GRAPHIC_DATA_LIST: Int = 10
const val Z_INDEX_MENU: Int = 20
const val Z_INDEX_ACTION_CONTAINER: Int = 30
const val Z_INDEX_ACTION_BODY: Int = 31
const val Z_INDEX_WAIT: Int = 40
const val Z_INDEX_LOADER: Int = 41
const val Z_INDEX_DIALOG: Int = 50
const val Z_INDEX_STATE_ALERT: Int = 50

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private const val NARROW_SCREEN_WIDTH = 400

val screenDPR: Double = window.devicePixelRatio
val scaleKoef: Double = if (screenDPR <= 1) {
    1.0
} else {
    0.5
}

//--- на мобильных устройствах это показывает ширину с учётом devicePixelRatio,
//--- причём на некоторых устройствах (особенно с iOS) глючит как outerWidth == 0, и тогда приходится использовать innerWidth
val scaledScreenWidth: Int = if (window.outerWidth > 0) {
    window.outerWidth
} else {
    window.innerWidth
}
val styleIsNarrowScreen: Boolean = (scaledScreenWidth <= NARROW_SCREEN_WIDTH)

fun getStyleIsTouchScreen(): Boolean {
    return js(
        """
	    ( 'ontouchstart' in window ) ||
		( navigator.maxTouchPoints > 0 ) ||
		( navigator.msMaxTouchPoints > 0 );
    """
    ).unsafeCast<Boolean>()
}

//--- especially for workaround of bug in SVG textspan dy
val isFirefox: Boolean = window.navigator.userAgent.contains("Firefox")

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- по умолчанию - тёмные иконки на светлом фоне
var styleDarkIcon: Boolean = true

//--- по умолчанию - иконки размером 36dp (пока только на toolbar'ах)
var styleIconSize: Int = 36  // м.б. 48

//--- суффикс наименовани типовой иконки material design
fun getStyleIconNameSuffix(): String = (if (styleDarkIcon) "black" else "white") + "_" + styleIconSize

//--- MAIN BACK ------------------------------------------------------------------------------------------------------------------------------------------------

val COLOR_MAIN_BACK_0: CSSColorValue = hsl(0, 0, 100)  // main background - white color for input fields, etc.

//--- different gray tones by default
private val MAIN_BACK_LIGHTNESS_0 = 97
private val MAIN_BACK_LIGHTNESS_1 = 94
private val MAIN_BACK_LIGHTNESS_2 = 88
private val MAIN_BACK_LIGHTNESS_3 = 82

var colorMainBack0 = hsl(0, 0, MAIN_BACK_LIGHTNESS_0)     // buttons
var colorMainBack1 = hsl(0, 0, MAIN_BACK_LIGHTNESS_1)     // panels, menus
var colorMainBack2 = hsl(0, 0, MAIN_BACK_LIGHTNESS_2)     // non-active tabs
var colorMainBack3 = hsl(0, 0, MAIN_BACK_LIGHTNESS_3)     // menu delimiters

//--- BORDER ---------------------------------------------------------------------------------------------------------------------------------------------------

var colorMainBorder = hsl(0, 0, 0)

var styleFormBorderRadius = (if (screenDPR <= 1.0) 0.2 else 0.4).cssRem
var styleButtonBorderRadius = (if (screenDPR <= 1.0) 0.2 else 0.4).cssRem
var styleInputBorderRadius = (if (screenDPR <= 1.0) 0.2 else 0.4).cssRem

//--- TEXT ---------------------------------------------------------------------------------------------------------------------------------------------------------------

val COLOR_MAIN_TEXT = hsl(0, 0, 0)

val COMMON_FONT_SIZE = 1.0.cssRem  //if(screenDPR <= 1.0 ) 1.0 else 1.0

//--- COMMON CONTROL -----------------------------------------------------------------------------------------------------------------------------------------------------

private val CONTROL_MARGIN = 0.1.cssRem

val CONTROL_PADDING = 0.3.cssRem
val CONTROL_TOP_DOWN_SIDE_PADDING = 0.1.cssRem
private val CONTROL_LEFT_RIGHT_SIDE_PADDING = 0.4.cssRem
private val CONTROL_BIG_PADDING = 0.95.cssRem

val styleControlTitleTextFontSize = COMMON_FONT_SIZE
val styleControlTextFontSize = COMMON_FONT_SIZE

val styleCommonButtonFontSize = COMMON_FONT_SIZE

//fun styleControlRadioTransform() = "scale(${if (!foatto.core_compose_web.getStyleIsNarrowScreen) foatto.core_compose_web.getCOMMON_FONT_SIZE * 1.5 else foatto.core_compose_web.getCOMMON_FONT_SIZE})"

val styleControlPadding = CONTROL_PADDING
val arrStyleControlTitlePadding: Array<CSSSize> = arrayOf(0.cssRem, CONTROL_PADDING, 0.cssRem, CONTROL_PADDING)
val styleIconButtonPadding = 0.cssRem
val styleTextButtonPadding = 0.2.cssRem
val getStyleStateServerButtonTextPadding: () -> CSSSize = { styleTextButtonPadding }
var styleStateServerButtonTextFontWeight = "normal"

val styleCommonEditorPadding = CONTROL_BIG_PADDING

//fun styleControlTooltipPadding() = "$CONTROL_PADDING $CONTROL_LEFT_RIGHT_SIDE_PADDING $CONTROL_PADDING $CONTROL_LEFT_RIGHT_SIDE_PADDING"

val arrStyleCommonMargin: Array<CSSSize> = arrayOf(0.cssRem, CONTROL_MARGIN, 0.cssRem, CONTROL_MARGIN)

//--- Button ---

var getColorButtonBack: () -> CSSColorValue = { colorMainBack0 }
var getColorButtonBorder: () -> CSSColorValue = { colorMainBorder }

//--- Checkbox ---

val styleCheckBoxWidth: CSSSize = 2.cssRem
val styleCheckBoxHeight: CSSSize = 2.cssRem
val getStyleCheckBoxBorder: () -> BorderData = { BorderData(colorMainBorder, LineStyle.Solid, 1.px, styleInputBorderRadius) }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

var colorCurrentAndHover = hsl(60, 100, 90)

//--- padding & margin for menu icon & tab panel
val menuTabPadMar = 0.3.cssRem

//--- FORM ---------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//var colorFormBack: () -> String = { foatto.core_compose_web.getColorMainBack1 }
//var styleFormLabelWeight: () -> String = { "normal" }
//var colorFormButtonBack: () -> String = { foatto.core_compose_web.style.getGetColorButtonBack() }
//var styleFormButtonBorder: () -> String = { "1px solid ${foatto.core_compose_web.style.getGetColorButtonBorder()}" }
//var colorFormActionButtonSaveBack: () -> String = { foatto.core_compose_web.style.getGetColorButtonBack() }
//var colorFormActionButtonOtherBack: () -> String = { foatto.core_compose_web.style.getGetColorButtonBack() }
//var styleFormActionButtonBorder: () -> String = { "1px solid ${foatto.core_compose_web.style.getGetColorButtonBorder()}" }
//
//fun styleFormEditBoxColumn(initSize: Int) = if (!foatto.core_compose_web.getStyleIsNarrowScreen) initSize else if (initSize <= foatto.core_compose_web.getScaledScreenWidth / 19) initSize else foatto.core_compose_web.getScaledScreenWidth / 19
//
////--- ! не убирать, так удобнее выравнивать label на форме, чем каждому тексту прописывать уникальный стиль
//fun styleFormRowPadding() = CONTROL_LEFT_RIGHT_SIDE_PADDING
//fun styleFormRowTopBottomPadding() = "0.1rem"
//fun styleFormLabelPadding() = "0.6rem"
//fun styleFormCheckboxAndRadioMargin() = "0.5rem"
//fun styleFileNameButtonPadding() = "0.95rem"
//fun styleFileNameButtonMargin() = "0.1rem"
//
//const val COLOR_FORM_SWITCH_BACK_ON = foatto.core_compose_web.style.getCOLOR_MAIN_BACK_0
//
////--- GRAPHIC ------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//const val COLOR_GRAPHIC_TIME_LINE = "hsl(180,100%,50%)"
//const val COLOR_GRAPHIC_LABEL_BACK = "hsl(60,100%,50%)"
//const val COLOR_GRAPHIC_LABEL_BORDER = "hsl(60,100%,25%)"
//const val COLOR_GRAPHIC_AXIS_DEFAULT = "hsl(0,0%,50%)"
//const val COLOR_GRAPHIC_DATA_BACK = "hsla(60,100%,50%,0.5)"
//
//var colorGraphicToolbarBack: () -> String = { foatto.core_compose_web.getColorMainBack1 }
//
//fun styleGraphicVisibilityTop() = "10.5rem"
//fun styleGraphicVisibilityMaxWidth() = if (foatto.core_compose_web.getStyleIsNarrowScreen) "85%" else "20rem"
//fun styleGraphicDataTop() = "10.8rem"
//fun styleGraphicDataMaxWidth() = if (foatto.core_compose_web.getStyleIsNarrowScreen) "85%" else "30rem"
//fun styleGraphicTimeLabelPadding() = "$CONTROL_PADDING $CONTROL_LEFT_RIGHT_SIDE_PADDING $CONTROL_PADDING $CONTROL_LEFT_RIGHT_SIDE_PADDING"
//
////--- XY -----------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//const val COLOR_XY_LABEL_BACK = "hsl(60,100%,50%)"
//const val COLOR_XY_LABEL_BORDER = "hsl(60,100%,25%)"
//
//const val COLOR_XY_LINE = "hsl(180,100%,50%)"
//
//const val COLOR_XY_DISTANCER = "hsl(30,100%,50%)"
//
//const val COLOR_XY_ZONE_CASUAL = "hsla(60,100%,50%,0.25)"    // полупрозрачный жёлтый
//const val COLOR_XY_ZONE_ACTUAL = "hsla(30,100%,50%,0.25)"    // полупрозрачный оранжевый
//const val COLOR_XY_ZONE_BORDER = "hsl(0,100%,50%)"          // красный
//
//var colorXyToolbarBack: () -> String = { foatto.core_compose_web.getColorMainBack1 }
//
//private val XY_PADDING = "0.2rem"
//private val XY_SIDE_PADDING = "0.4rem"
//
//fun styleXyDistancerPadding() = "$XY_PADDING $XY_SIDE_PADDING $XY_PADDING $XY_SIDE_PADDING"
//fun styleXyTextPadding() = "$XY_PADDING $XY_SIDE_PADDING $XY_PADDING $XY_SIDE_PADDING"

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

fun StyleScope.setBorder(
    color: CSSColorValue,
    lineStyle: LineStyle = LineStyle.Solid,
    width: CSSSize = 1.px,
    arrRadius: Array<CSSSize>,
) {
    border {
        color(color)
        style(lineStyle)
        width(width)
    }
    borderRadius(
        topLeft = arrRadius[0],
        topRight = arrRadius[1],
        bottomRight = arrRadius[2],
        bottomLeft = arrRadius[3],
    )
}

fun StyleScope.setBorder(
    color: CSSColorValue,
    lineStyle: LineStyle = LineStyle.Solid,
    arrWidth: Array<CSSSize>,
    arrRadius: Array<CSSSize>,
) {
    border {
        color(color)
        style(lineStyle)
    }
    borderWidth(
        top = arrWidth[0],
        right = arrWidth[1],
        bottom = arrWidth[2],
        left = arrWidth[3],
    )
    borderRadius(
        topLeft = arrRadius[0],
        topRight = arrRadius[1],
        bottomRight = arrRadius[2],
        bottomLeft = arrRadius[3],
    )
}

fun StyleScope.setBorder(
    arrColor: Array<CSSColorValue>,
    lineStyle: LineStyle = LineStyle.Solid,
    arrWidth: Array<CSSSize>,
    arrRadius: Array<CSSSize>,
) {
    borderColor(
        top = arrColor[0],
        right = arrColor[1],
        bottom = arrColor[2],
        left = arrColor[3],
    )
    border {
        style(lineStyle)
    }
    borderWidth(
        top = arrWidth[0],
        right = arrWidth[1],
        bottom = arrWidth[2],
        left = arrWidth[3],
    )
    borderRadius(
        topLeft = arrRadius[0],
        topRight = arrRadius[1],
        bottomRight = arrRadius[2],
        bottomLeft = arrRadius[3],
    )
}

fun StyleScope.setBorder(
    color: CSSColorValue,
    lineStyle: LineStyle = LineStyle.Solid,
    width: CSSSize = 1.px,
    radius: CSSSize,
) {
    border {
        color(color)
        style(lineStyle)
        width(width)
    }
    borderRadius(radius)
}

fun StyleScope.setBorder(borderData: BorderData) {
    border {
        color(borderData.color)
        style(borderData.style)
        width(borderData.width)
    }
    borderRadius(borderData.radius)
}

fun StyleScope.setPaddings(arrPadding: Array<CSSSize>) {
    paddingTop(arrPadding[0])
    paddingRight(arrPadding[1])
    paddingBottom(arrPadding[2])
    paddingLeft(arrPadding[3])
}

fun StyleScope.setMargins(arrMargin: Array<CSSSize>) {
    marginTop(arrMargin[0])
    marginRight(arrMargin[1])
    marginBottom(arrMargin[2])
    marginLeft(arrMargin[3])
}
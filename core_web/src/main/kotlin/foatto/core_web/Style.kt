package foatto.core_web

import kotlinx.browser.localStorage
import kotlinx.browser.window

//--- MAIN BACK ----------------------------------------------------------------------------------------------------------------------------------------------------------

const val COLOR_MAIN_BACK_0 = "hsl(0,0%,100%)"     // main background - white color for input fields, etc.

//--- different gray tones by default
var colorMainBack0 = "hsl(0,0%,97%)"     // buttons
var colorMainBack1 = "hsl(0,0%,94%)"     // panels, menus
var colorMainBack2 = "hsl(0,0%,88%)"     // non-active tabs
var colorMainBack3 = "hsl(0,0%,82%)"     // menu delimiters

//--- BORDER -------------------------------------------------------------------------------------------------------------------------------------------------------------

var colorMainBorder = "hsl(120,41%,69%)"

//--- TEXT ---------------------------------------------------------------------------------------------------------------------------------------------------------------

const val COLOR_MAIN_TEXT = "hsl(0,0%,0%)"

//--- BUTTON -------------------------------------------------------------------------------------------------------------------------------------------------------------

var colorButtonBack = colorMainBack0
var colorButtonBorder = colorMainBorder

//--- LOGON FORM ---------------------------------------------------------------------------------------------------------------------------------------------------------

var colorLogonBackAround = colorMainBack1
var colorLogonBackCenter = colorMainBack2
var colorLogonBorder = colorMainBorder
var colorLogonButtonBack = colorButtonBack
var colorLogonButtonBorder = colorMainBorder

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

var colorCurrentAndHover = "hsl(60,100%,90%)"

//--- MENU ---------------------------------------------------------------------------------------------------------------------------------------------------------------

const val IS_HIDDEN_MENU_BAR = "is_hidden_menu_bar"

var colorMenuBack = colorMainBack1
var colorMenuBorder = colorMainBorder
var colorMenuDelimiter = colorMainBack1

val MENU_DELIMITER = "&nbsp;".repeat(60)

//--- TABLE --------------------------------------------------------------------------------------------------------------------------------------------------------------

var colorTableRowBack0 = "hsl(0,0%,100%)"
var colorTableRowBack1 = "hsl(0,0%,97%)"

var colorGroupBack0 = "hsl(120,35%,90%)"
var colorGroupBack1 = "hsl(120,100%,95%)"

//--- FORM ---------------------------------------------------------------------------------------------------------------------------------------------------------------

const val COLOR_FORM_SWITCH_BACK_ON = COLOR_MAIN_BACK_0

//--- GRAPHIC ------------------------------------------------------------------------------------------------------------------------------------------------------------

const val COLOR_GRAPHIC_TIME_LINE = "hsl(180,100%,50%)"
const val COLOR_GRAPHIC_LABEL_BACK = "hsl(60,100%,50%)"
const val COLOR_GRAPHIC_LABEL_BORDER = "hsl(60,100%,25%)"
const val COLOR_GRAPHIC_AXIS_DEFAULT = "hsl(0,0%,50%)"
const val COLOR_GRAPHIC_DATA_BACK = "hsla(60,100%,50%,0.5)"

//--- XY -----------------------------------------------------------------------------------------------------------------------------------------------------------------

const val COLOR_XY_LABEL_BACK = "hsl(60,100%,50%)"
const val COLOR_XY_LABEL_BORDER = "hsl(60,100%,25%)"

const val COLOR_XY_LINE = "hsl(180,100%,50%)"

const val COLOR_XY_DISTANCER = "hsl(30,100%,50%)"

const val COLOR_XY_ZONE_CASUAL = "hsla(60,100%,50%,0.25)"    // полупрозрачный жёлтый
const val COLOR_XY_ZONE_ACTUAL = "hsla(30,100%,50%,0.25)"    // полупрозрачный оранжевый
const val COLOR_XY_ZONE_BORDER = "hsl(0,100%,50%)"    // красный

//--- DIALOG -------------------------------------------------------------------------------------------------------------------------------------------------------------

var colorDialogBack = "hsla(0,0%,0%,0.75)"
var colorDialogBorder = colorMainBorder
var colorDialogBackCenter = colorMainBack1
var colorDialogButtonBack = colorButtonBack
var colorDialogButtonBorder = colorMainBorder

//--- WAIT ---------------------------------------------------------------------------------------------------------------------------------------------------------------

var colorWaitBack = "hsla(0,0%,100%,0.7)"
var colorWaitLoader0 = "hsl(60,100%,80%)"
var colorWaitLoader1 = "hsl(60,100%,85%)"
var colorWaitLoader2 = "hsl(60,100%,90%)"
var colorWaitLoader3 = "hsl(60,100%,95%)"

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------





private val NARROW_SCREEN_WIDTH = 400
val screenDPR = window.devicePixelRatio

//--- на мобильных устройствах это показывает ширину с учётом devicePixelRatio,
//--- причём на некоторых устройствах (особенно с iOS) глючит как outerWidth == 0, и тогда приходится использовать innerWidth
val scaledScreenWidth = if (window.outerWidth > 0) {
    window.outerWidth
} else {
    window.innerWidth
}
val styleIsNarrowScreen = (scaledScreenWidth <= NARROW_SCREEN_WIDTH)

fun styleIsTouchScreen(): Boolean {
    return js(
        """
	    ( 'ontouchstart' in window ) ||
		( navigator.maxTouchPoints > 0 ) ||
		( navigator.msMaxTouchPoints > 0 );
    """
    ).unsafeCast<Boolean>()
}

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

val BORDER_RADIUS = "${if (screenDPR <= 1.0) 0.2 else 0.1}rem"
const val BORDER_RADIUS_SMALL = "0.1rem"

//--- Common Control

//--- кое-где используется как чисто числовое выражение, поэтому определяем без rem
val COMMON_FONT_SIZE = 1.0  //if( screenDPR <= 1.0 ) 1.0 else 1.0

private val CONTROL_MARGIN = "${if (screenDPR <= 1.0) 0.1 else 0.1}rem"

private val CONTROL_PADDING = "0.3rem" //"${if( screenDPR <= 1.0 ) 0.3 else 0.3}rem"
private val CONTROL_TOP_DOWN_SIDE_PADDING = "${if (screenDPR <= 1.0) 0.1 else 0.1}rem"
private val CONTROL_LEFT_RIGHT_SIDE_PADDING = "${if (screenDPR <= 1.0) 0.4 else 0.4}rem"
private val CONTROL_BIG_PADDING = "${if (screenDPR <= 1.0) 0.95 else 0.95}rem"

fun styleControlTitleTextFontSize() = "${COMMON_FONT_SIZE}rem"
fun styleControlTextFontSize() = "${COMMON_FONT_SIZE}rem"
fun styleCommonButtonFontSize() = "${COMMON_FONT_SIZE}rem"

fun styleControlCheckBoxTransform() = "scale(${COMMON_FONT_SIZE * 2.0})"
fun styleControlRadioTransform() = "scale(${if (!styleIsNarrowScreen) COMMON_FONT_SIZE * 1.5 else COMMON_FONT_SIZE})"

fun styleControlPadding() = CONTROL_PADDING
fun styleControlTitlePadding() = "0 $CONTROL_PADDING 0 $CONTROL_PADDING"
fun styleIconButtonPadding() = "${if (screenDPR <= 1.0) 0.0 else 0.0}rem"       // 0.2
fun styleTextButtonPadding() = "${if (screenDPR <= 1.0) 0.2 else 0.2}rem"       // 1.0
fun styleCommonEditorPadding() = CONTROL_BIG_PADDING
fun styleControlTooltipPadding() = "$CONTROL_PADDING $CONTROL_LEFT_RIGHT_SIDE_PADDING $CONTROL_PADDING $CONTROL_LEFT_RIGHT_SIDE_PADDING"
fun styleTableGridCellTypePadding() = "$CONTROL_TOP_DOWN_SIDE_PADDING $CONTROL_PADDING $CONTROL_TOP_DOWN_SIDE_PADDING $CONTROL_PADDING"

fun styleCommonMargin() = "0 $CONTROL_MARGIN 0 $CONTROL_MARGIN"

//--- Dialog

fun styleDialogCellPadding() = "1.0rem"
fun styleDialogControlPadding() = "0.4rem 0"
fun styleDialogButtonPadding() = "1.0rem ${if (!styleIsNarrowScreen) 8 else scaledScreenWidth / 48}rem"
//fun styleDialogButtonMargin() = "1.0rem 0 0 0"

//--- MenuBar

var styleIsHiddenMenuBar = localStorage.getItem(IS_HIDDEN_MENU_BAR)?.toBooleanStrictOrNull() ?: true

//--- Logon

fun styleLogonTextLen() = if (!styleIsNarrowScreen) 40 else scaledScreenWidth / 16
fun styleLogonCellPadding() = "1.0rem"

//!!! проверить как будет выглядеть с другими logo !!! (у лого снизу padding побольше - для вертикального центрирования)
fun styleLogonLogoPadding() = "0.4rem 0 1.0rem 0"
fun styleLogonControlPadding() = "0.4rem 0"
fun styleLogonCheckBoxMargin() = "0.4rem"           // опытным путём checkbox выровнен по левому краю input-box'ов

//--- в настольной версии - меньше чем ширина полей ввода, в мобильной - практически равна им
fun styleLogonButtonPadding() = "1.0rem ${if (!styleIsNarrowScreen) 8 else scaledScreenWidth / 48}rem"
fun styleLogonButtonMargin() = "1.0rem 0 0 0"       // отодвигаем logon button от остальных контролов на один ряд символов вниз

//--- padding & margin for menu icon & tab panel

private fun sMenuTabPadMar() = if (screenDPR <= 1.0) 0.3 else 0.3

//--- Menu Icon

fun styleMenuIconButtonMargin() = "0 ${sMenuTabPadMar()}rem 0 0"

//--- Tab Panel

fun styleTabPanelPadding() = "${sMenuTabPadMar()}rem ${sMenuTabPadMar()}rem 0 ${sMenuTabPadMar()}rem"

//--- tab-combo - только на устройствах с узким экраном
private fun sTabComboMargin() = sMenuTabPadMar()
fun styleTabComboTextLen() = scaledScreenWidth / 16
fun styleTabComboFontSize() = "${COMMON_FONT_SIZE}rem"
fun styleTabComboPadding() = "${if (screenDPR <= 1.0) 0.55 else 0.55}rem"
fun styleTabComboMargin() = "0 0 ${sTabComboMargin()}rem 0"
fun styleTabCloserButtonMargin() = "0 0 ${sTabComboMargin()}rem ${sTabComboMargin()}rem"

private fun sTabTitleTopPadding() = if (screenDPR <= 1.0) 0.7 else 0.7
private fun sTabTitleSidePadding() = if (screenDPR <= 1.0) 0.6 else 0.6
private fun sTabTitleBottomPadding() = if (screenDPR <= 1.0) 0.7 else 0.7
private fun sTabCloserTopPadding() = if (screenDPR <= 1.0) 1.2 else 1.2
private fun sTabCloserSidePadding() = if (screenDPR <= 1.0) 0.4 else 0.4
private fun sTabCloserBottomPadding() = if (screenDPR <= 1.0) 1.2 else 1.2
fun styleTabCurrentTitlePadding() = "${sTabTitleTopPadding()}rem ${sTabTitleSidePadding()}rem ${sTabTitleBottomPadding()}rem ${sTabTitleSidePadding()}rem"
fun styleTabOtherTitlePadding() = "${sTabTitleTopPadding()}rem ${sTabTitleSidePadding()}rem ${sTabTitleBottomPadding()}rem ${sTabTitleSidePadding()}rem"
fun styleTabButtonFontSize() = "${COMMON_FONT_SIZE}rem"
fun styleTabCurrentCloserPadding() = "${sTabCloserTopPadding()}rem ${sTabCloserSidePadding()}rem ${sTabCloserBottomPadding()}rem ${sTabCloserSidePadding()}rem"
fun styleTabOtherCloserPadding() = "${sTabCloserTopPadding()}rem ${sTabCloserSidePadding()}rem ${sTabCloserBottomPadding()}rem ${sTabCloserSidePadding()}rem"

//--- Popup Menu

private fun styleMenuItemTopBottomPad() = if (screenDPR <= 1.0) 0.8 else 0.8
private fun styleMenuItemSidePad_0() = if (screenDPR <= 1.0) 0.0 else 0.0   // 0.1
private fun styleMenuItemSidePad_1() = if (screenDPR <= 1.0) 1.0 else 1.0
private fun styleMenuItemSidePad_2() = if (screenDPR <= 1.0) 2.0 else 2.0

fun styleMenuStartTop() = "${if (screenDPR <= 1.0) 3.4 else 3.4}rem"
fun styleMenuStartPadding() = "${if (screenDPR <= 1.0) 1.0 else 1.0}rem " +
    "${if (screenDPR <= 1.0) 1.0 else 1.0}rem " +
    "${if (screenDPR <= 1.0) 1.0 else 1.0}rem " +
    "${if (screenDPR <= 1.0) 1.0 else 1.0}rem"

fun styleMenuWidth() = if (styleIsNarrowScreen) "85%" else if (!styleIsHiddenMenuBar) "20rem" else "auto"
fun styleMenuFontSize() = "${if (screenDPR <= 1.0) 1.0 else 1.0}rem"
fun styleMenuItemPadding_0() = "${styleMenuItemTopBottomPad()}rem ${styleMenuItemSidePad_0()}rem ${styleMenuItemTopBottomPad()}rem ${styleMenuItemSidePad_0()}rem"
fun styleMenuItemPadding_1() = "${styleMenuItemTopBottomPad()}rem ${styleMenuItemSidePad_1()}rem ${styleMenuItemTopBottomPad()}rem ${styleMenuItemSidePad_1()}rem"
fun styleMenuItemPadding_2() = "${styleMenuItemTopBottomPad()}rem ${styleMenuItemSidePad_2()}rem ${styleMenuItemTopBottomPad()}rem ${styleMenuItemSidePad_2()}rem"

//--- Table

fun styleTableFindEditLength() = scaledScreenWidth / (if (screenDPR <= 1.0) 48 else 24)
fun styleTableFindEditorFontSize() = "${COMMON_FONT_SIZE}rem"

fun styleTableTextFontSize() = "${if (!styleIsNarrowScreen) 1.0 else 0.8}rem"

private fun sTablePageBarTopBottomPadding() = if (screenDPR <= 1.0) 0.3 else 0.3
fun styleTablePageBarPadding() = "${sTablePageBarTopBottomPadding()}rem $CONTROL_PADDING ${sTablePageBarTopBottomPadding()}rem $CONTROL_PADDING"
fun styleTablePageButtonWidth(buttonCount: Int) =
    if (styleIsNarrowScreen) {
        val buttonWidth = when (buttonCount) {
            4 -> 3.4    // 5.2 - слишком крупно и глуповато, используется всего один раз
            5 -> 3.4    // 4.1 - используется всего один раз
            6 -> 3.4
            7 -> 2.9
            8 -> 2.5
            else -> 2.0
        }
        "${buttonWidth}rem"
    } else {
        "6.0rem"
    }

fun styleTablePageButtonFontSize(buttonCount: Int) =
    if (styleIsNarrowScreen) {
        val fontSize = when (buttonCount) {
            4 -> 1.7    // 2.6 - слишком крупно и глуповато, используется всего один раз
            5 -> 1.7    // 2.0 - используется всего один раз
            6 -> 1.7
            7 -> 1.4
            8 -> 1.2
            else -> 1.0
        }
        "${fontSize}rem"
    } else {
        "2.6rem"
    }

//--- Form

fun styleFormEditBoxColumn(initSize: Int) = if (!styleIsNarrowScreen) initSize else if (initSize <= scaledScreenWidth / 19) initSize else scaledScreenWidth / 19

//--- ! не убирать, так удобнее выравнивать label на форме, чем каждому тексту прописывать уникальный стиль
fun styleFormRowPadding() = CONTROL_LEFT_RIGHT_SIDE_PADDING
fun styleFormRowTopBottomPadding() = "0.1rem"
fun styleFormLabelPadding() = "0.6rem"
fun styleFormCheckboxAndRadioMargin() = "0.5rem"
fun styleFileNameButtonPadding() = "0.95rem"
fun styleFileNameButtonMargin() = "0.1rem"

//--- Graphic

fun styleGraphicVisibilityTop() = "${if (screenDPR <= 1.0) 9.0 else 9.0}rem"
fun styleGraphicVisibilityMaxWidth() = if (styleIsNarrowScreen) "85%" else "20rem"
fun styleGraphicDataTop() = "${if (screenDPR <= 1.0) 9.0 else 9.0}rem"
fun styleGraphicDataMaxWidth() = if (styleIsNarrowScreen) "85%" else "30rem"
fun styleGraphicTimeLabelPadding() = "$CONTROL_PADDING $CONTROL_LEFT_RIGHT_SIDE_PADDING $CONTROL_PADDING $CONTROL_LEFT_RIGHT_SIDE_PADDING"

//--- Xy

private val XY_PADDING = "${if (screenDPR <= 1.0) 0.2 else 0.2}rem"
private val XY_SIDE_PADDING = "${if (screenDPR <= 1.0) 0.4 else 0.4}rem"

fun styleXyDistancerPadding() = "$XY_PADDING $XY_SIDE_PADDING $XY_PADDING $XY_SIDE_PADDING"
fun styleXyTextPadding() = "$XY_PADDING $XY_SIDE_PADDING $XY_PADDING $XY_SIDE_PADDING"

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

fun getHSL(hue: Int, saturation: Int, lightness: Int) = "hsl($hue,$saturation%,$lightness%)"
fun getHSLA(hue: Int, saturation: Int, lightness: Int, alpha: Double) = "hsl($hue,$saturation%,$lightness%,$alpha)"
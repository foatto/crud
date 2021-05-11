package foatto.core_web

import kotlinx.browser.window

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

private const val COLOR_MAIN_BACK_0 = "#ffffff"     // основной белый фон
private const val COLOR_MAIN_BACK_1 = "#f0f0f0"     // фон панелей с кнопками, кнопок и меню, цвет фона для чётных строк таблиц и прочего чередования
private const val COLOR_MAIN_BACK_2 = "#e0e0e0"     // фон неактивных вкладок
private const val COLOR_MAIN_BACK_3 = "#d0d0d0"     // фон разделителей в меню

private const val COLOR_MAIN_ROW_0_BACK = "#ffffff"
private const val COLOR_MAIN_ROW_1_BACK = "#f6f6f6" // д.б. между COLOR_MAIN_BACK_0 и COLOR_MAIN_BACK_1

//--- зелёная группировка
private const val COLOR_MAIN_BACK_GROUP_0 = "#e0f0e0"
private const val COLOR_MAIN_BACK_GROUP_1 = "#f0fff0"
//--- фиолетовая группировка
//private const val COLOR_MAIN_BACK_GROUP_0 = "#e0e0f0"
//private const val COLOR_MAIN_BACK_GROUP_1 = "#f0f0ff"

private const val COLOR_MAIN_BACK_HOVER_0 = "#ffffd0"
//private const val COLOR_MAIN_BACK_HOVER_1 = "#ffffc0"

private const val COLOR_MAIN_BORDER = "#90d090" //"#a0a0a0"     // цвет рамок,бордюров, границ

private const val COLOR_MAIN_TEXT = "#000000"

//private const val COLOR_MAIN_MODAL_BACK = "#000000"

private const val COLOR_MAIN_WAIT = "255, 255, 255"

private const val COLOR_MAIN_LOADER_0 = "#ffff90"
private const val COLOR_MAIN_LOADER_1 = "#ffffb0"
private const val COLOR_MAIN_LOADER_2 = "#ffffd0"
private const val COLOR_MAIN_LOADER_3 = "#fffff0"

private const val COLOR_MAIN_GOOD_HOVER = "#008000"
private const val COLOR_MAIN_GOOD_CURRENT = "#004000"
private const val COLOR_MAIN_GOOD_OTHER = "#002000"

private const val COLOR_MAIN_NEUTRAL_HOVER = "#000080"
private const val COLOR_MAIN_NEUTRAL_CURRENT = "#000040"
private const val COLOR_MAIN_NEUTRAL_OTHER = "#000020"

private const val COLOR_MAIN_BAD_HOVER = "#800000"
private const val COLOR_MAIN_BAD_CURRENT = "#400000"    //"#800000"
private const val COLOR_MAIN_BAD_OTHER = "#200000"

//--- General --------------------------------------------------------------------------------------------------------------------------------------------------------------------------

const val COLOR_BACK = COLOR_MAIN_BACK_0

const val COLOR_PANEL_BACK = COLOR_MAIN_BACK_1

const val COLOR_TAB_BACK_CURRENT = COLOR_MAIN_BACK_1
const val COLOR_TAB_BACK_OTHER = COLOR_MAIN_BACK_2
const val COLOR_TAB_BORDER = COLOR_MAIN_BORDER

const val COLOR_BUTTON_BACK = COLOR_MAIN_BACK_1     // COLOR_MAIN_BORDER
const val COLOR_BUTTON_BORDER = COLOR_MAIN_BORDER

const val COLOR_TEXT = COLOR_MAIN_TEXT

//const val COLOR_MODAL_BACK = COLOR_MAIN_MODAL_BACK

const val COLOR_WAIT = COLOR_MAIN_WAIT
const val COLOR_LOADER_0 = COLOR_MAIN_LOADER_0
const val COLOR_LOADER_1 = COLOR_MAIN_LOADER_1
const val COLOR_LOADER_2 = COLOR_MAIN_LOADER_2
const val COLOR_LOADER_3 = COLOR_MAIN_LOADER_3

//--- Menu -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------

const val COLOR_MENU_BACK = COLOR_MAIN_BACK_1
const val COLOR_MENU_BACK_HOVER = COLOR_MAIN_BACK_HOVER_0   //COLOR_MAIN_BACK_HOVER_1
const val COLOR_MENU_BORDER = COLOR_MAIN_BORDER
const val COLOR_MENU_DELIMITER = COLOR_MAIN_BACK_3

val MENU_DELIMITER = "&nbsp;".repeat(60)

//--- LOGON FORM -----------------------------------------------------------------------------------------------------------------------------------------------------------------------

const val COLOR_LOGON_BACK = COLOR_MAIN_BACK_1

//--- TABLE ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------

const val COLOR_TABLE_SELECTOR_CANCEL = COLOR_MAIN_BAD_CURRENT

const val COLOR_TABLE_FIND_CLEAR = COLOR_MAIN_BAD_CURRENT

const val COLOR_TABLE_ROW_SELECTED_BACK = COLOR_MAIN_BACK_HOVER_0

const val COLOR_TABLE_ROW_0_BACK = COLOR_MAIN_ROW_0_BACK
const val COLOR_TABLE_ROW_1_BACK = COLOR_MAIN_ROW_1_BACK

var colorGroupBack0 = COLOR_MAIN_BACK_GROUP_0
var colorGroupBack1 = COLOR_MAIN_BACK_GROUP_1

const val COLOR_TABLE_BUTTON = COLOR_MAIN_GOOD_CURRENT

//--- FORM -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- GRAPHIC --------------------------------------------------------------------------------------------------------------------------------------------------------------------------

const val COLOR_GRAPHIC_TIME_LINE = "#00ffff"       // безусловная линия/прямоугольник

const val COLOR_GRAPHIC_LABEL_BACK = "#ffff00"
const val COLOR_GRAPHIC_LABEL_BORDER = "#808000"
const val COLOR_GRAPHIC_LABEL_TEXT = COLOR_MAIN_TEXT

const val COLOR_GRAPHIC_TITLE = COLOR_MAIN_TEXT
const val COLOR_GRAPHIC_AXIS_DEFAULT = "#808080"

//--- XY -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//const val COLOR_XY_ZONE_CASUAL = "#40ffff00" // полупрозрачный жёлтый
//const val COLOR_XY_ZONE_ACTUAL = "#40ff8000" // полупрозрачный оранжевый

const val COLOR_XY_LABEL_BACK = "#ffff00"
const val COLOR_XY_LABEL_BORDER = "#808000"
const val COLOR_XY_LABEL_TEXT = COLOR_MAIN_TEXT

const val COLOR_XY_LINE = "#00ffff"       // безусловная линия/прямоугольник

const val COLOR_XY_DISTANCER = "#ff8000"    // линейка

const val COLOR_XY_ZONE_CASUAL = "#ffff0040"    // полупрозрачный жёлтый
const val COLOR_XY_ZONE_ACTUAL = "#ff800040"    // полупрозрачный оранжевый
const val COLOR_XY_ZONE_BORDER = "#ff0000ff"    // красный

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

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

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

val BORDER_RADIUS = "${if (screenDPR <= 1.0) 0.2 else 0.1}rem"
const val BORDER_RADIUS_SMALL = "0.1rem"

//--- Common Control

//--- кое-где используется как чисто числовое выражение, поэтому определяем без rem
private const val COMMON_FONT_SIZE = 1.0  //if( screenDPR <= 1.0 ) 1.0 else 1.0

private val CONTROL_MARGIN = "${if (screenDPR <= 1.0) 0.1 else 0.1}rem"
private val CONTROL_PADDING = "0.3rem" //"${if( screenDPR <= 1.0 ) 0.3 else 0.3}rem"
private val CONTROL_SIDE_PADDING = "${if (screenDPR <= 1.0) 0.4 else 0.4}rem"
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
fun styleControlTooltipPadding() = "$CONTROL_PADDING $CONTROL_SIDE_PADDING $CONTROL_PADDING $CONTROL_SIDE_PADDING"

fun styleCommonMargin() = "0 $CONTROL_MARGIN 0 $CONTROL_MARGIN"

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

fun styleMenuWidth() = if (!styleIsNarrowScreen) "auto" else "85%"
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
fun styleFormRowPadding() = CONTROL_SIDE_PADDING
fun styleFormRowTopBottomPadding() = "0.1rem"
fun styleFormLabelPadding() = "0.6rem"
fun styleFormCheckboxAndRadioMargin() = "0.5rem"
fun styleFileNameButtonPadding() = "0.95rem"
fun styleFileNameButtonMargin() = "0.1rem"

//--- Graphic

fun styleGraphicCheckBoxMargin() = "0 $CONTROL_PADDING 0 $CONTROL_SIDE_PADDING"
fun styleGraphicCheckBoxLabelPadding() = "0 $CONTROL_SIDE_PADDING 0 $CONTROL_PADDING"
fun styleGraphicTimeLabelPadding() = "$CONTROL_PADDING $CONTROL_SIDE_PADDING $CONTROL_PADDING $CONTROL_SIDE_PADDING"

//--- Xy

private val XY_PADDING = "${if (screenDPR <= 1.0) 0.2 else 0.2}rem"
private val XY_SIDE_PADDING = "${if (screenDPR <= 1.0) 0.4 else 0.4}rem"

fun styleXyDistancerPadding() = "$XY_PADDING $XY_SIDE_PADDING $XY_PADDING $XY_SIDE_PADDING"
fun styleXyTextPadding() = "$XY_PADDING $XY_SIDE_PADDING $XY_PADDING $XY_SIDE_PADDING"



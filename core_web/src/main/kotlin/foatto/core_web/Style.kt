package foatto.core_web

import kotlinx.browser.window

//--- Z-INDEX ------------------------------------------------------------------------------------------------------------------------------------------------------------

const val Z_INDEX_TABLE_CAPTION = 1
const val Z_INDEX_TABLE_POPUP = 2
const val Z_INDEX_GRAPHIC_VISIBILITY_LIST = 10
const val Z_INDEX_GRAPHIC_DATA_LIST = 10
const val Z_INDEX_MENU = 20
const val Z_INDEX_ACTION_CONTAINER = 30
const val Z_INDEX_ACTION_BODY = 31
const val Z_INDEX_WAIT = 40
const val Z_INDEX_LOADER = 41
const val Z_INDEX_DIALOG = 50
const val Z_INDEX_STATE_ALERT = 50

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

//--- по умолчанию - тёмные иконки на светлом фоне
var styleDarkIcon = true

//--- по умолчанию - иконки размером 36dp (пока только на toolbar'ах)
var styleIconSize = 36  // м.б. 48

//--- суффикс наименовани типовой иконки material design
fun styleIconNameSuffix() = (if (styleDarkIcon) "black" else "white") + "_" + styleIconSize

//--- MAIN BACK ----------------------------------------------------------------------------------------------------------------------------------------------------------

const val COLOR_MAIN_BACK_0 = "hsl(0,0%,100%)"     // main background - white color for input fields, etc.

//--- different gray tones by default
private val MAIN_BACK_LIGHTNESS_0 = 97
private val MAIN_BACK_LIGHTNESS_1 = 94
private val MAIN_BACK_LIGHTNESS_2 = 88
private val MAIN_BACK_LIGHTNESS_3 = 82

var colorMainBack0 = getHSL(0, 0, MAIN_BACK_LIGHTNESS_0)     // buttons
var colorMainBack1 = getHSL(0, 0, MAIN_BACK_LIGHTNESS_1)     // panels, menus
var colorMainBack2 = getHSL(0, 0, MAIN_BACK_LIGHTNESS_2)     // non-active tabs
var colorMainBack3 = getHSL(0, 0, MAIN_BACK_LIGHTNESS_3)     // menu delimiters

//--- BORDER -------------------------------------------------------------------------------------------------------------------------------------------------------------

var colorMainBorder: () -> String = { getHSL(0, 0, 0) }

var styleFormBorderRadius = "${if (screenDPR <= 1.0) 0.2 else 0.4}rem"
var styleButtonBorderRadius = "${if (screenDPR <= 1.0) 0.2 else 0.4}rem"
var styleInputBorderRadius = "${if (screenDPR <= 1.0) 0.2 else 0.4}rem"

//--- TEXT ---------------------------------------------------------------------------------------------------------------------------------------------------------------

val COLOR_MAIN_TEXT = getHSL(0, 0, 0)

//--- somewhere used as numerical value, define without "rem"
val COMMON_FONT_SIZE = 1.0  //if( screenDPR <= 1.0 ) 1.0 else 1.0

//--- COMMON CONTROL -----------------------------------------------------------------------------------------------------------------------------------------------------

private val CONTROL_MARGIN = "0.1rem"

val CONTROL_PADDING = "0.3rem"
private val CONTROL_TOP_DOWN_SIDE_PADDING = "0.1rem"
private val CONTROL_LEFT_RIGHT_SIDE_PADDING = "0.4rem"
private val CONTROL_BIG_PADDING = "0.95rem"

fun styleControlTitleTextFontSize() = "${COMMON_FONT_SIZE}rem"
fun styleControlTextFontSize() = "${COMMON_FONT_SIZE}rem"
fun styleCommonButtonFontSize() = "${COMMON_FONT_SIZE}rem"

fun styleControlRadioTransform() = "scale(${if (!styleIsNarrowScreen) COMMON_FONT_SIZE * 1.5 else COMMON_FONT_SIZE})"

fun styleControlPadding() = CONTROL_PADDING
fun styleControlTitlePadding() = "0 $CONTROL_PADDING 0 $CONTROL_PADDING"
fun styleIconButtonPadding() = "0.0rem"
fun styleTextButtonPadding() = "0.2rem"
var styleStateServerButtonTextPadding: () -> String = { styleTextButtonPadding() }
var styleStateServerButtonTextFontWeight = "normal"

fun styleCommonEditorPadding() = CONTROL_BIG_PADDING

fun styleControlTooltipPadding() = "$CONTROL_PADDING $CONTROL_LEFT_RIGHT_SIDE_PADDING $CONTROL_PADDING $CONTROL_LEFT_RIGHT_SIDE_PADDING"
fun styleTableGridCellTypePadding() = "$CONTROL_TOP_DOWN_SIDE_PADDING $CONTROL_PADDING $CONTROL_TOP_DOWN_SIDE_PADDING $CONTROL_PADDING"

fun styleCommonMargin() = "0 $CONTROL_MARGIN 0 $CONTROL_MARGIN"

//--- Button ---

var colorButtonBack: () -> String = { colorMainBack0 }
var colorButtonBorder: () -> String = { colorMainBorder() }

//--- Checkbox ---

var styleCheckBoxWidth = "2rem"
var styleCheckBoxHeight = "2rem"
fun styleCheckBoxBorder() = "1px solid ${colorMainBorder()}"

//--- WAIT ---------------------------------------------------------------------------------------------------------------------------------------------------------------

var colorWaitBack = "hsla(0,0%,100%,0.7)"
var colorWaitLoader0 = "hsl(60,100%,80%)"
var colorWaitLoader1 = "hsl(60,100%,85%)"
var colorWaitLoader2 = "hsl(60,100%,90%)"
var colorWaitLoader3 = "hsl(60,100%,95%)"

//--- DIALOG -------------------------------------------------------------------------------------------------------------------------------------------------------------

var colorDialogBack = "hsla(0,0%,0%,0.75)"
var colorDialogBorder: () -> String = { colorMainBorder() }
var colorDialogBackCenter: () -> String = { colorMainBack1 }
var colorDialogButtonBack: () -> String = { colorButtonBack() }
var colorDialogButtonBorder: () -> String = { colorMainBorder() }

fun styleDialogCellPadding() = "1.0rem"
fun styleDialogControlPadding() = "0.4rem 0"
fun styleDialogButtonPadding() = "1.0rem ${if (!styleIsNarrowScreen) 8 else scaledScreenWidth / 48}rem"
//fun styleDialogButtonMargin() = "1.0rem 0 0 0"

//--- LOGON FORM ---------------------------------------------------------------------------------------------------------------------------------------------------------

var colorLogonBackAround: () -> String = { colorMainBack1 }
var colorLogonBackCenter: () -> String = { colorMainBack2 }
var colorLogonBorder: () -> String = { colorMainBorder() }
var colorLogonButtonBack: () -> String = { colorButtonBack() }
var colorLogonButtonText = COLOR_MAIN_TEXT
var colorLogonButtonBorder: () -> String = { colorButtonBorder() }

var styleLogonTopExpanderContent = "&nbsp;"
var styleLogonLogo = "logo.png"
var styleLogonLogoContent = ""

val styleLogonCellPadding = "${if (!styleIsNarrowScreen) "2.0rem" else "1.0rem"} 2.0rem"
val styleLogonLogoPadding = "0.4rem 0 1.0rem 0"
val styleLogonControlPadding = "0.4rem 0"
val styleLogonCheckBoxMargin = "0rem 0.5rem 0rem 0rem"
var styleLogonButtonPadding: () -> String = { "1.0rem ${if (!styleIsNarrowScreen) 8 else scaledScreenWidth / 48}rem" }
val styleLogonButtonMargin = "1.0rem 0 ${if (!styleIsNarrowScreen) "1.0rem" else "0"} 0"

fun styleLogonTextLen() = if (!styleIsNarrowScreen) 40 else scaledScreenWidth / 16
var styleLogonButtonText = "Вход"
var styleLogonButtonFontWeight = "normal"

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

var colorCurrentAndHover = "hsl(60,100%,90%)"

//--- padding & margin for menu icon & tab panel
private val menuTabPadMar = 0.3

//--- MENUS --------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- Main Menu ---

var styleIsHiddenMenuBar = true
var styleMenuBar = ""
var colorMenuCloserBack = colorMainBack1
var colorMenuCloserButtonBack = COLOR_MAIN_TEXT
var colorMenuCloserButtonText = COLOR_MAIN_BACK_0

var styleMainMenuTop = ""
var styleTopBar = ""

var colorMainMenuBack: () -> String = { colorMainBack1 }      // м.б. прозрачным из-за фонового рисунка фона главного меню
var colorPopupMenuBack: () -> String = { colorMainBack1 }    // обычно всегда имеет сплошной цвет
var colorMenuTextDefault = COLOR_MAIN_TEXT
var colorMenuBorder: () -> String = { colorMainBorder() }
var colorMenuDelimiter: () -> String = { colorMainBack3 }

var colorMenuBackHover0 = colorCurrentAndHover
var colorMenuTextHover0: String? = null

var colorMenuBackHoverN = colorCurrentAndHover
var colorMenuTextHoverN: String? = null

val MENU_DELIMITER = "&nbsp;".repeat(60)

fun styleMenuStartTop() = if (styleIsNarrowScreen) {
    "3.4rem"
} else {
    "${if (screenDPR <= 1.0) 3.7 else 3.4}rem"
}

fun styleMenuIconButtonMargin() = if (styleIsNarrowScreen) {
    "0 ${menuTabPadMar}rem 0 0"
} else {
    "${menuTabPadMar}rem ${menuTabPadMar}rem 0 0"
}

//--- Main & Popup Menus ---

const val styleMenuStartPadding = "1.0rem 1.0rem 1.0rem 1.0rem"

private fun styleMenuItemTopBottomPad(level: Int) = if (styleIsNarrowScreen) {
    arrayOf(0.8, 0.6, 0.4)[level]
} else {
    arrayOf(0.8, 0.4, 0.2)[level]
}

private val arrStyleMenuItemSidePad = arrayOf(0.0, 1.0, 2.0)
private val arrMenuFontSize = arrayOf(1.0, 0.9, 0.8)

fun styleMenuWidth() = if (styleIsNarrowScreen) "85%" else if (!styleIsHiddenMenuBar) "17rem" else "auto"
fun styleMenuItemPadding(level: Int) = "${styleMenuItemTopBottomPad(level)}rem ${arrStyleMenuItemSidePad[level]}rem ${styleMenuItemTopBottomPad(level)}rem ${arrStyleMenuItemSidePad[level]}rem"
fun styleMenuFontSize(level: Int) = "${arrMenuFontSize[level]}rem"

//--- APP CONTROL --------------------------------------------------------------------------------------------------------------------------------------------------------

var styleAppControlPadding: () -> String = { "0" }

//--- TAB PANEL ----------------------------------------------------------------------------------------------------------------------------------------------------------

var colorTabPanelBack = COLOR_MAIN_BACK_0
var styleTabPanelPadding: () -> String = { "${menuTabPadMar}rem ${menuTabPadMar}rem 0 ${menuTabPadMar}rem" }

var colorTabCurrentBack: () -> String = { colorMainBack1 }
var styleTabCurrentTitleBorderLeft: () -> String = { "1px solid ${colorMainBorder()}" }
var styleTabCurrentTitleBorderTop: () -> String = { "1px solid ${colorMainBorder()}" }
var styleTabCurrentTitleBorderRight: () -> String = { "none" }
var styleTabCurrentTitleBorderBottom: () -> String = { "none" }
var styleTabCurrentCloserBorderLeft: () -> String = { "none" }
var styleTabCurrentCloserBorderTop: () -> String = { "1px solid ${colorMainBorder()}" }
var styleTabCurrentCloserBorderRight: () -> String = { "1px solid ${colorMainBorder()}" }
var styleTabCurrentCloserBorderBottom: () -> String = { "1px solid $colorMainBack1" }

var colorTabOtherBack: () -> String = { colorMainBack2 }
var styleTabOtherTitleBorderLeft: () -> String = { "1px solid ${colorMainBorder()}" }
var styleTabOtherTitleBorderTop: () -> String = { "1px solid ${colorMainBorder()}" }
var styleTabOtherTitleBorderRight: () -> String = { "none" }
var styleTabOtherTitleBorderBottom: () -> String = { "1px solid ${colorMainBorder()}" }
var styleTabOtherCloserBorderLeft: () -> String = { "none" }
var styleTabOtherCloserBorderTop: () -> String = { "1px solid ${colorMainBorder()}" }
var styleTabOtherCloserBorderRight: () -> String = { "1px solid ${colorMainBorder()}" }
var styleTabOtherCloserBorderBottom: () -> String = { "1px solid ${colorMainBorder()}" }

//--- tab-combo - только на устройствах с узким экраном
private val tabComboMargin = menuTabPadMar
fun styleTabComboTextLen() = scaledScreenWidth / (if (styleIsNarrowScreen) 16 else 64)
fun styleTabComboFontSize() = "${COMMON_FONT_SIZE}rem"
fun styleTabComboPadding() = "0.55rem"
fun styleTabComboMargin() = "0 0 ${tabComboMargin}rem 0"
fun styleTabCloserButtonMargin() = "0 0 ${tabComboMargin}rem ${tabComboMargin}rem"

var styleTabCurrentTitlePadding = "0.7rem 0.6rem 0.7rem 0.6rem"
var styleTabOtherTitlePadding = "0.7rem 0.6rem 0.7rem 0.6rem"
var styleTabButtonFontSize = "${COMMON_FONT_SIZE}rem"
var styleTabCurrentCloserPadding = "${if (screenDPR <= 1.0) 1.4 else 1.2}rem 0.4rem ${if (screenDPR <= 1.0) 1.4 else 1.2}rem 0.4rem"
var styleTabOtherCloserPadding = "${if (screenDPR <= 1.0) 1.4 else 1.2}rem 0.4rem ${if (screenDPR <= 1.0) 1.4 else 1.2}rem 0.4rem"

//--- TABLE --------------------------------------------------------------------------------------------------------------------------------------------------------------

var colorTableHeaderBack: () -> String = { colorMainBack1 }
var colorTableToolbarBack: () -> String = { colorMainBack1 }
var colorTablePagebarBack: () -> String = { colorMainBack1 }

var colorTableFindButtonBack: () -> String = { colorButtonBack() }
var styleTableFindEditorBorderRadius: () -> String = { styleInputBorderRadius }
var styleTableFindButtonBorderRadius: () -> String = { styleButtonBorderRadius }
var styleTableFindControlMargin: () -> String = { styleCommonMargin() }
fun styleTableFindEditLength() = scaledScreenWidth / (if (screenDPR <= 1.0) 64 else 24)
val styleTableFindEditorFontSize = "${COMMON_FONT_SIZE}rem"
fun styleTableFindEditorPadding() = when (styleIconSize) {
    36 -> "0.56rem"
    48 -> "0.95rem"
    else -> "0.56rem"   // пусть лучше будет поменьше, чем раздирать тулбар
}

var colorToolbarButtonBack: () -> String = { colorButtonBack() }
var colorRefreshButtonBack: () -> String = { colorButtonBack() }
var styleToolbarButtonBorder: () -> String = { "1px solid ${colorButtonBorder()}" }

var styleTableCaptionBack: () -> String = { colorMainBack1 }
var styleTableCaptionPadding: () -> String = { styleControlPadding() }
var styleTableCaptionBorderLeft: () -> String = { "0.5px solid $${colorMainBorder()}" }
var styleTableCaptionBorderTop: () -> String = { "1px solid ${colorMainBorder()}" }
var styleTableCaptionBorderRight: () -> String = { "0.5px solid ${colorMainBorder()}" }
var styleTableCaptionBorderBottom: () -> String = { "1px solid ${colorMainBorder()}" }
var styleTableCaptionAlignH: () -> String = { "center" }    // flex-start
var styleTableCaptionAlignV: () -> String = { "center" }    // flex-start
var styleTableCaptionFontSize: () -> String = { styleTableTextFontSize() }
var styleTableCaptionFontWeight: () -> String = { "normal" }

var colorGroupBack0: () -> String = { COLOR_MAIN_BACK_0 }
var colorGroupBack1: () -> String = { COLOR_MAIN_BACK_0 }

var colorTableRowBack0: () -> String = { COLOR_MAIN_BACK_0 }
var colorTableRowBack1: () -> String = { COLOR_MAIN_BACK_0 }

var colorTableRowHover: () -> String = { colorCurrentAndHover }

var styleTableTextFontSize: () -> String = { "${if (!styleIsNarrowScreen) 1.0 else 0.8}rem" }

var colorTablePageBarCurrentBack: () -> String = { colorButtonBack() }
var styleTablePageBarOtherBorder: () -> String = { "1px solid ${colorButtonBorder()}" }

private const val tablePageBarTopBottomPadding = 0.3
fun styleTablePageBarPadding() = "${tablePageBarTopBottomPadding}rem $CONTROL_PADDING ${tablePageBarTopBottomPadding}rem $CONTROL_PADDING"
var styleTablePageButtonWidth: (Int) -> String = { buttonCount ->
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
}

var styleTablePageButtonFontSize: (Int) -> String = { buttonCount ->
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
}

//--- FORM ---------------------------------------------------------------------------------------------------------------------------------------------------------------

var colorFormBack: () -> String = { colorMainBack1 }
var styleFormLabelWeight: () -> String = { "normal" }
var colorFormButtonBack: () -> String = { colorButtonBack() }
var styleFormButtonBorder: () -> String = { "1px solid ${colorButtonBorder()}" }
var colorFormActionButtonSaveBack: () -> String = { colorButtonBack() }
var colorFormActionButtonOtherBack: () -> String = { colorButtonBack() }
var styleFormActionButtonBorder: () -> String = { "1px solid ${colorButtonBorder()}" }

fun styleFormEditBoxColumn(initSize: Int) = if (!styleIsNarrowScreen) initSize else if (initSize <= scaledScreenWidth / 19) initSize else scaledScreenWidth / 19

//--- ! не убирать, так удобнее выравнивать label на форме, чем каждому тексту прописывать уникальный стиль
fun styleFormRowPadding() = CONTROL_LEFT_RIGHT_SIDE_PADDING
fun styleFormRowTopBottomPadding() = "0.1rem"
fun styleFormLabelPadding() = "0.6rem"
fun styleFormCheckboxAndRadioMargin() = "0.5rem"
fun styleFileNameButtonPadding() = "0.95rem"
fun styleFileNameButtonMargin() = "0.1rem"

const val COLOR_FORM_SWITCH_BACK_ON = COLOR_MAIN_BACK_0

//--- GRAPHIC ------------------------------------------------------------------------------------------------------------------------------------------------------------

const val COLOR_GRAPHIC_TIME_LINE = "hsl(180,100%,50%)"
const val COLOR_GRAPHIC_LABEL_BACK = "hsl(60,100%,50%)"
const val COLOR_GRAPHIC_LABEL_BORDER = "hsl(60,100%,25%)"
const val COLOR_GRAPHIC_AXIS_DEFAULT = "hsl(0,0%,50%)"
const val COLOR_GRAPHIC_DATA_BACK = "hsla(60,100%,50%,0.5)"

var colorGraphicToolbarBack: () -> String = { colorMainBack1 }

//--- XY -----------------------------------------------------------------------------------------------------------------------------------------------------------------

const val COLOR_XY_LABEL_BACK = "hsl(60,100%,50%)"
const val COLOR_XY_LABEL_BORDER = "hsl(60,100%,25%)"

const val COLOR_XY_LINE = "hsl(180,100%,50%)"

const val COLOR_XY_DISTANCER = "hsl(30,100%,50%)"

const val COLOR_XY_ZONE_CASUAL = "hsla(60,100%,50%,0.25)"    // полупрозрачный жёлтый
const val COLOR_XY_ZONE_ACTUAL = "hsla(30,100%,50%,0.25)"    // полупрозрачный оранжевый
const val COLOR_XY_ZONE_BORDER = "hsl(0,100%,50%)"          // красный

var colorXyToolbarBack: () -> String = { colorMainBack1 }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------





//--- Graphic

fun styleGraphicVisibilityTop() = "10.5rem"
fun styleGraphicVisibilityMaxWidth() = if (styleIsNarrowScreen) "85%" else "20rem"
fun styleGraphicDataTop() = "10.8rem"
fun styleGraphicDataMaxWidth() = if (styleIsNarrowScreen) "85%" else "30rem"
fun styleGraphicTimeLabelPadding() = "$CONTROL_PADDING $CONTROL_LEFT_RIGHT_SIDE_PADDING $CONTROL_PADDING $CONTROL_LEFT_RIGHT_SIDE_PADDING"

//--- Xy

private val XY_PADDING = "0.2rem"
private val XY_SIDE_PADDING = "0.4rem"

fun styleXyDistancerPadding() = "$XY_PADDING $XY_SIDE_PADDING $XY_PADDING $XY_SIDE_PADDING"
fun styleXyTextPadding() = "$XY_PADDING $XY_SIDE_PADDING $XY_PADDING $XY_SIDE_PADDING"

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

fun getHSL(hue: Int, saturation: Int, lightness: Int) = "hsl($hue,$saturation%,$lightness%)"
fun getHSLA(hue: Int, saturation: Int, lightness: Int, alpha: Double) = "hsl($hue,$saturation%,$lightness%,$alpha)"

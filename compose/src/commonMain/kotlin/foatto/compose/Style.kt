package foatto.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.hsl

//--- по умолчанию - тёмные иконки на светлом фоне кнопок,
//--- но в material 3 design (да и в прочих дизайнах) - контраст к основному фону - светлые иконки/текст на тёмном фоне иконок
var styleToolbarDarkIcon: Boolean = true
var styleOtherDarkIcon: Boolean = true

//--- по умолчанию - иконки размером 36dp (пока только на toolbar'ах, в остальных местах 48)
var styleToolbarIconSize: Int = 36
var styleOtherIconSize: Int = 48

//--- суффикс наименовани типовой иконки material design
fun getStyleToolbarIconNameSuffix(): String = (if (styleToolbarDarkIcon) "black" else "white") + "_" + styleToolbarIconSize + "dp"
fun getStyleOtherIconNameSuffix(): String = (if (styleOtherDarkIcon) "black" else "white") + "_" + styleOtherIconSize + "dp"

//--- different gray tones by default
private const val MAIN_BACK_LIGHTNESS_0 = 0.97f
private const val MAIN_BACK_LIGHTNESS_1 = 0.94f
private const val MAIN_BACK_LIGHTNESS_2 = 0.88f
private const val MAIN_BACK_LIGHTNESS_3 = 0.82f

var colorMainBack0: Color = Color.hsl(0.0f, 0.0f, 1.0f)  // main background - white color for input fields, etc.
var colorMainBack1: Color = Color.hsl(0.0f, 0.0f, MAIN_BACK_LIGHTNESS_0)     // buttons
var colorMainBack2: Color = Color.hsl(0.0f, 0.0f, MAIN_BACK_LIGHTNESS_1)     // panels, menus
var colorMainBack3: Color = Color.hsl(0.0f, 0.0f, MAIN_BACK_LIGHTNESS_2)     // non-active tabs
var colorMainBack4: Color = Color.hsl(0.0f, 0.0f, MAIN_BACK_LIGHTNESS_3)     // menu delimiters

var colorMainText: Color = Color.Black
var colorMainBorder: Color = hsl(0f, 0f, 0.9f)

var getColorButtonBack: () -> Color = { colorMainBack1 }


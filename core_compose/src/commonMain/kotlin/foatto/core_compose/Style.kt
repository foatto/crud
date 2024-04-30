package foatto.core_compose

import androidx.compose.ui.graphics.Color

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

var colorMainBorder: Color = Color.hsl(0f, 0f, 0.9f)

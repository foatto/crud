package foatto.core_compose

//--- по умолчанию - тёмные иконки на светлом фоне
var styleDarkIcon: Boolean = true

//--- по умолчанию - иконки размером 36dp (пока только на toolbar'ах, в остальных местах 48)
var styleIconSize: Int = 36  // м.б. 48

//--- суффикс наименовани типовой иконки material design
fun getStyleIconNameSuffix(): String = (if (styleDarkIcon) "black" else "white") + "_" + styleIconSize + "dp"

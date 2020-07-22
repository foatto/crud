package foatto.core.app

interface iControl {

    fun onRequestFocus()

    fun onScreenSizeChanged()

    fun onFontSizeChanged( fontSizeInc: Int )

    fun onScreenWidthModeChanged()

    fun onScaleKoefChanged()

    fun onTabClose()

}

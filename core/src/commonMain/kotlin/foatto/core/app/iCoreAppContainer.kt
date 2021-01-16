package foatto.core.app

interface iCoreAppContainer {

    companion object {
        const val BASE_FONT_SIZE = 12   // base font size (for scaleKoef == 1)
        const val MAP_REFRESH_PERIOD = 12 * 30 * 24 * 60 * 60
        const val UP_BITMAP_MAP_MODE = "bitmap_map_mode"
    }
}

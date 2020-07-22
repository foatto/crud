package foatto.core.app

interface iCoreAppContainer {

    companion object {
        //--- базовый размер шрифта (для scaleKoef == 1)
        const val BASE_FONT_SIZE = 12

        //--- наименования и значения параметров


        //--- (пока) одному важному пользователю не понравилось автозакрытие формы запуска отчёта после его генерации
        const val UP_DISABLE_REPORT_AUTOCLOSE = "disable_report_autoclose"

        //--- период обновления карт
        const val MAP_REFRESH_PERIOD = 12 * 30 * 24 * 60 * 60   // один год - вряд ли карты обновляются чаще

        const val UP_BITMAP_MAP_MODE = "bitmap_map_mode"  // тип показываемой растровой карты
    }
}

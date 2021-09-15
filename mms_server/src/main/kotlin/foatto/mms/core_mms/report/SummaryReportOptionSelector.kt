package foatto.mms.core_mms.report

import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.iColumn

class SummaryReportOptionSelector {
    lateinit var columnKeepPlaceForComment: ColumnBoolean
        private set
    lateinit var columnOutLiquidLevelMainContainerUsing: ColumnBoolean
        private set
    lateinit var columnOutTemperature: ColumnBoolean
        private set
    lateinit var columnOutDensity: ColumnBoolean
        private set
    lateinit var columnOutTroubles: ColumnBoolean
        private set

    fun fillColumns(userConfig: UserConfig, tableSOS: String, alFormColumn: MutableList<iColumn>) {

        columnKeepPlaceForComment = ColumnBoolean(tableSOS, "_keep_place_for_comment", "Оставлять место под комментарии", false).apply {
            isVirtual = true
            setSavedDefault(userConfig)
        }
        columnOutLiquidLevelMainContainerUsing = ColumnBoolean(tableSOS, "_out_liquid_level_main_container_using", "Выводить показания расхода основных ёмкостей", false).apply {
            isVirtual = true
            setSavedDefault(userConfig)
        }
        columnOutTemperature = ColumnBoolean(tableSOS, "_out_temperature", "Выводить показания температуры", false).apply {
            isVirtual = true
            setSavedDefault(userConfig)
        }
        columnOutDensity = ColumnBoolean(tableSOS, "_out_density", "Выводить показания плотности", true).apply {
            isVirtual = true
            setSavedDefault(userConfig)
        }
        columnOutTroubles = ColumnBoolean(tableSOS, "_out_troubles", "Выводить неисправности", true).apply {
            isVirtual = true
            setSavedDefault(userConfig)
        }

        //----------------------------------------------------------------------------------------------------------------------

        alFormColumn.add(columnKeepPlaceForComment)
        alFormColumn.add(columnOutLiquidLevelMainContainerUsing)
        alFormColumn.add(columnOutTemperature)
        alFormColumn.add(columnOutDensity)
        alFormColumn.add(columnOutTroubles)
    }
}

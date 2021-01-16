package foatto.mms.core_mms.report

import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean

class mSummary : mUODGP() {

    lateinit var columnOutTemperature: ColumnBoolean
        private set
    lateinit var columnOutDensity: ColumnBoolean
        private set

    lateinit var sos: SumOptionSelector
        private set

    override fun defineOptionsColumns(userConfig: UserConfig) {
        super.defineOptionsColumns(userConfig)

        columnOutTemperature = ColumnBoolean(tableName, "out_temperature", "Выводить показания температуры", false).apply {
            isVirtual = true
            setSavedDefault(userConfig)
        }
        columnOutDensity = ColumnBoolean(tableName, "out_density", "Выводить показания плотности", true).apply {
            isVirtual = true
            setSavedDefault(userConfig)
        }
    }

    override fun addOptionsColumns(userConfig: UserConfig) {
        super.addOptionsColumns(userConfig)

        alFormColumn.add(columnOutTemperature)
        alFormColumn.add(columnOutDensity)

        sos = SumOptionSelector()
        sos.fillColumns(userConfig, tableName, alFormColumn)
    }
}

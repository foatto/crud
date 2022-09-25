package foatto.mms.core_mms.report

import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDouble

class mBNGRE : mOP() {

    lateinit var columnPeriodBeginValue: ColumnDouble
        private set

    override fun defineOptionsColumns(userConfig: UserConfig) {
        super.defineOptionsColumns(userConfig)

        columnPeriodBeginValue = ColumnDouble(modelTableName, "_period_begin_value", "Остаток на начало периода").apply {
            isVirtual = true
        }

    }

    override fun addOptionsColumns(userConfig: UserConfig) {
        super.addOptionsColumns(userConfig)

        alFormColumn.add(columnPeriodBeginValue)
    }
}

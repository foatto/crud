package foatto.mms.core_mms.report

import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.iColumn

class SumOptionSelector {

    lateinit var columnSumOnly: ColumnBoolean
        private set
    lateinit var columnSumUser: ColumnBoolean
        private set
    lateinit var columnSumObject: ColumnBoolean
        private set

    fun fillColumns(userConfig: UserConfig, tableSOS: String, alFormColumn: MutableList<iColumn>) {

        columnSumOnly = ColumnBoolean(tableSOS, "sum_only", "Выводить только суммы", false).apply {
            isVirtual = true
            setSavedDefault(userConfig)
        }
        columnSumUser = ColumnBoolean(tableSOS, "sum_user", "Выводить суммы по владельцам", true).apply {
            isVirtual = true
            setSavedDefault(userConfig)
        }
        columnSumObject = ColumnBoolean(tableSOS, "sum_object", "Выводить суммы по объектам", true).apply {
            isVirtual = true
            setSavedDefault(userConfig)
            addFormVisible(columnSumUser, true, setOf(1))
        }

        //----------------------------------------------------------------------------------------------------------------------

        alFormColumn.add(columnSumOnly)
        alFormColumn.add(columnSumUser)
        alFormColumn.add(columnSumObject)
    }
}

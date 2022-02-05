package foatto.ts.core_ts

import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.system.mClient

class mTSClient: mClient() {

    lateinit var columnControlEnabled: ColumnBoolean

    override fun addColumnDefinitions() {
        super.addColumnDefinitions()

        columnControlEnabled = ColumnBoolean(modelTableName, "is_control_enabled", "Управление устройствами", false).apply {
            isVirtual = true
        }
    }

    override fun addColumnsToTableAndForm() {
        super.addColumnsToTableAndForm()

        addTableColumn(columnControlEnabled)
        alFormColumn += columnControlEnabled
    }
}
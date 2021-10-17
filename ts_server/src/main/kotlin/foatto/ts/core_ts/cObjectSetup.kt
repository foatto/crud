package foatto.ts.core_ts

import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataDateTimeInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.ts.core_ts.calc.ObjectState
import foatto.ts.iTSApplication

class cObjectSetup : cStandart() {

    override fun definePermission() {
        alPermission.add(Pair(PERM_ACCESS, "01 Access"))
        alPermission.add(Pair(PERM_FORM, "02 Form"))
    }

    override fun generateFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        val objectConfig = (application as iTSApplication).getObjectConfig(userConfig, hmParentData["ts_object"]!!)
        val objectState = ObjectState.getState(stm, objectConfig)

        objectState.lastDateTime?.let { lastDateTime ->
            val mos = model as mObjectSetup
            (hmColumnData[mos.columnDateTime] as DataDateTimeInt).setDateTime(lastDateTime)

            mos.tmColumnSetup.forEach { (showPos, column) ->
                when (column) {
                    is ColumnString -> {
                        (hmColumnData[column] as DataString).text = objectState.tmSetupValue[showPos] ?: "(неизвестно)"
                    }
                    is ColumnBoolean -> {
                        (hmColumnData[column] as DataBoolean).value = objectState.tmSetupValue[showPos]?.toBooleanStrictOrNull() ?: false
                    }
                }
            }
        }
    }

}
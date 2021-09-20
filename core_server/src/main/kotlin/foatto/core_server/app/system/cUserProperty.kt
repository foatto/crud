package foatto.core_server.app.system

import foatto.core.app.UP_DECIMAL_DIVIDER
import foatto.core.app.UP_IS_USE_THOUSANDS_DIVIDER
import foatto.core.app.UP_TIME_OFFSET
import foatto.core.link.FormData
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataRadioButton
import foatto.core_server.app.server.data.iData

class cUserProperty : cStandart() {

    override fun definePermission() {
        alPermission.add(Pair(PERM_ACCESS, "01 Access"))
        alPermission.add(Pair(PERM_FORM, "02 Form"))
        alPermission.add(Pair(PERM_ADD, "03 Add"))
    }

    override fun generateFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        val mup = model as mUserProperty

        (hmColumnData[mup.columnTimeShift] as DataComboBox).intValue = userConfig.upTimeOffset

        (hmColumnData[mup.columnDivideThousands] as DataBoolean).value = userConfig.getUserProperty(UP_IS_USE_THOUSANDS_DIVIDER)?.toBoolean() ?: true

        (hmColumnData[mup.columnDividerChar] as DataRadioButton).intValue = if (userConfig.getUserProperty(UP_DECIMAL_DIVIDER) == ",") {
            1
        } else {
            0
        }
    }

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val alColumnList = mutableListOf<iColumn>()
        alColumnList.addAll(model.alFormHiddenColumn)
        alColumnList.addAll(model.alFormColumn)

        val hmColumnData = mutableMapOf<iColumn, iData>()

        val id = getIDFromParam()!!

        //--- ошибки ввода в форме
        val returnURL = checkInput(id, alFormData, alColumnList, hmColumnData, hmOut)
        //--- урло на возврат с ошибкой
        if (returnURL != null) {
            return returnURL
        }

        val mup = model as mUserProperty

        userConfig.saveUserProperty(conn, UP_TIME_OFFSET, (hmColumnData[mup.columnTimeShift] as DataComboBox).intValue.toString())
        userConfig.saveUserProperty(conn, UP_IS_USE_THOUSANDS_DIVIDER, (hmColumnData[mup.columnDivideThousands] as DataBoolean).value.toString())
        userConfig.saveUserProperty(
            conn,
            UP_DECIMAL_DIVIDER,
            if ((hmColumnData[mup.columnDividerChar] as DataRadioButton).intValue == 0) {
                "."
            } else {
                ","
            }
        )

        return "#"
    }
}
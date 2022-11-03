package foatto.core_server.app.server

import foatto.core.link.FormData
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.iData

open class cAbstractForm : cStandart() {

    //--- для передачи данных между родительским и наследуемым doSave
    protected var hmColumnData = mutableMapOf<iColumn, iData>()

    override fun definePermission() {
        alPermission.add(Pair(PERM_ACCESS, "01 Access"))
        alPermission.add(Pair(PERM_TABLE, "02 Table"))
        alPermission.add(Pair(PERM_ADD, "03 Add"))
        alPermission.add(Pair(PERM_FORM, "04 Form"))
    }

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val alColumnList = mutableListOf<iColumn>()
        alColumnList.addAll(model.alFormHiddenColumn)
        alColumnList.addAll(model.alFormColumn)

        val id = getIdFromParam()!!

        //--- ошибки ввода в форме
        val returnURL = checkInput(id, alFormData, alColumnList, hmColumnData, hmOut)

        //--- если нет ошибок, то сохраним значения saved-default-values
        if (returnURL == null) {
            for (column in hmColumnData.keys) {
                if (column.isSavedDefault) {
                    column.saveDefault(application, conn, userConfig, hmColumnData)
                }
            }
        }
        return returnURL
    }
}

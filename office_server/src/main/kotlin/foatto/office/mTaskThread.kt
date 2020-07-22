package foatto.office

import foatto.app.CoreSpringController
import foatto.core.link.FormPinMode
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnFile
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.ColumnTime3Int
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mTaskThread : mAbstract() {

    companion object {
        val ALERT_TAG = "task_thread"
    }

    lateinit var columnTaskThreadMessage: ColumnString
        private set

    //--- оповещать только при добавлении сообщения
    override fun getAddAlertTag(): String? {
        return ALERT_TAG
    }

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        val parentTaskID: Int = hmParentData["office_task_out"] ?: hmParentData["office_task_in"] ?: hmParentData["office_task_out_archive"] ?: hmParentData["office_task_in_archive"]!!

        //----------------------------------------------------------------------------------------

        tableName = "OFFICE_task_thread"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")
        columnUser = ColumnInt(tableName, "user_id", userConfig.userID)

        //----------------------------------------------------------------------------------------------------------------------

        val columnTask = ColumnInt(tableName, "task_id", parentTaskID)

        val columnTaskThreadDate = ColumnDate3Int(tableName, "ye", "mo", "da", "Дата")
        val columnTaskThreadTime = ColumnTime3Int(tableName, "ho", "mi", null, "Время")

        columnTaskThreadMessage = ColumnString(tableName, "message", "Обсуждение", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)
        columnTaskThreadMessage.formPinMode = FormPinMode.OFF

        val columnFile = ColumnFile(tableName, "file_id", "Файлы")
        columnFile.formPinMode = FormPinMode.OFF

        //----------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnUser!!)
        alTableHiddenColumn.add(columnTask)

        addTableColumn(columnTaskThreadDate)
        addTableColumn(columnTaskThreadTime)
        addTableColumn(columnTaskThreadMessage)
        addTableColumn(columnFile)

        //--- поля для сортировки
        alTableSortColumn.add(columnTaskThreadDate)
        alTableSortDirect.add("DESC")
        alTableSortColumn.add(columnTaskThreadTime)
        alTableSortDirect.add("DESC")

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnUser!!)
        alFormHiddenColumn.add(columnTask)
        alFormHiddenColumn.add(columnTaskThreadDate)
        alFormHiddenColumn.add(columnTaskThreadTime)

        alFormColumn.add(columnTaskThreadMessage)
        alFormColumn.add(columnFile)

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!
        hmParentColumn["office_task_out"] = columnTask
        hmParentColumn["office_task_in"] = columnTask
        hmParentColumn["office_task_out_archive"] = columnTask
        hmParentColumn["office_task_in_archive"] = columnTask
    }
}

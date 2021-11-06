package foatto.office.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstractReport
import foatto.sql.CoreAdvancedStatement

class mTask : mAbstractReport() {

    lateinit var columnReportTask: ColumnInt
        private set

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(
        application: iApplication,
        aStm: CoreAdvancedStatement,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        val parentTaskID = hmParentData["office_task_out"] ?: hmParentData["office_task_in"]

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "OFFICE_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnTaskID = ColumnInt("OFFICE_task", "id")
        columnReportTask = ColumnInt(tableName, "task_id", columnTaskID, parentTaskID)

        val columnTaskSubj = ColumnString("OFFICE_task", "subj", "Тема поручения", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID)
        alFormHiddenColumn.add(columnReportTask)

        alFormColumn.add(columnTaskSubj)
    }
}

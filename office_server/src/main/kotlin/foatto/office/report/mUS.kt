package foatto.office.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstractReport
import foatto.sql.CoreAdvancedStatement

class mUS : mAbstractReport() {

    lateinit var columnReportUser: ColumnInt
        private set
    lateinit var columnSumOnly: ColumnBoolean
        private set

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

        modelTableName = "OFFICE_report"

//----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

//----------------------------------------------------------------------------------------------------------------------

        val columnUserID = ColumnInt("SYSTEM_users", "id")
        columnReportUser = ColumnInt(modelTableName, "user_id", columnUserID, 0 /*userConfig.getUserID()*/)

        val columnUserName = ColumnString("SYSTEM_users", "full_name", "По пользователю", STRING_COLUMN_WIDTH).apply {
            selectorAlias = "system_user_people"
            addSelectorColumn(columnReportUser, columnUserID)
            addSelectorColumn(this)
        }

        columnSumOnly = ColumnBoolean(modelTableName, "sum_only", "Выводить только суммы", false)

//----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn += columnId
        alFormHiddenColumn += columnReportUser

        alFormColumn += columnUserName
        alFormColumn += columnSumOnly
    }
}

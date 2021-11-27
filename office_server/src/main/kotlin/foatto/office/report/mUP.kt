package foatto.office.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstractReport
import foatto.sql.CoreAdvancedStatement

class mUP : mAbstractReport() {

    lateinit var columnReportUser: ColumnInt
        private set
    lateinit var columnReportBegDate: ColumnDate3Int
        private set
    lateinit var columnReportEndDate: ColumnDate3Int
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

        tableName = "OFFICE_report"

//----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

//----------------------------------------------------------------------------------------------------------------------

        val columnUserID = ColumnInt("SYSTEM_users", "id")
        columnReportUser = ColumnInt(tableName, "user_id", columnUserID, 0 /*userConfig.getUserID()*/)

        val columnUserName = ColumnString("SYSTEM_users", "full_name", "По пользователю", STRING_COLUMN_WIDTH).apply {
            selectorAlias = "system_user_people"
            addSelectorColumn(columnReportUser, columnUserID)
            addSelectorColumn(this)
        }

        columnReportBegDate = ColumnDate3Int(tableName, "beg_ye", "beg_mo", "beg_da", "Начало периода")
        columnReportEndDate = ColumnDate3Int(tableName, "end_ye", "end_mo", "end_da", "Конец периода")

//----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn += columnID
        alFormHiddenColumn += columnReportUser

        alFormColumn += columnUserName
        alFormColumn += columnReportBegDate
        alFormColumn += columnReportEndDate
    }
}
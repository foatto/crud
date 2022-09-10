package foatto.office.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstractReport
import foatto.sql.CoreAdvancedConnection

class mUP : mAbstractReport() {

    lateinit var columnReportUser: ColumnInt
        private set
    lateinit var columnReportBegDate: ColumnDate3Int
        private set
    lateinit var columnReportEndDate: ColumnDate3Int
        private set

    override fun init(
        application: iApplication,
        aConn: CoreAdvancedConnection,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

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

        columnReportBegDate = ColumnDate3Int(modelTableName, "beg_ye", "beg_mo", "beg_da", "Начало периода")
        columnReportEndDate = ColumnDate3Int(modelTableName, "end_ye", "end_mo", "end_da", "Конец периода")

//----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn += columnId
        alFormHiddenColumn += columnReportUser

        alFormColumn += columnUserName
        alFormColumn += columnReportBegDate
        alFormColumn += columnReportEndDate
    }
}

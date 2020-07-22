package foatto.mms.core_mms.report

import foatto.app.CoreSpringController
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.mAbstractReport
import foatto.mms.core_mms.UODGSelector
import foatto.sql.CoreAdvancedStatement

class mUODG : mAbstractReport() {

    lateinit var uodg: UODGSelector
        private set

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        initReportCapAndSignature(aliasConfig, userConfig)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID!!)

        //----------------------------------------------------------------------------------------------------------------------

        uodg = UODGSelector()
        uodg.fillColumns(tableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn)

        addCapAndSignatureColumns()
    }
}

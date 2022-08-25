package foatto.mms.core_mms.report

import foatto.core_server.app.iApplication
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

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)

        //----------------------------------------------------------------------------------------------------------------------

        uodg = UODGSelector()
        uodg.fillColumns(application, modelTableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn)

    }
}

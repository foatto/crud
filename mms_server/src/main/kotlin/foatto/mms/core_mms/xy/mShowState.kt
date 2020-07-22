package foatto.mms.core_mms.xy

import foatto.app.CoreSpringController
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.mAbstract
import foatto.mms.core_mms.ObjectSelector
import foatto.sql.CoreAdvancedStatement

class mShowState : mAbstract() {

    private lateinit var os: ObjectSelector

    val columnObject: ColumnInt
        get() = os.columnObject

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID!!)

        //----------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
        os.fillColumns(this, true, true, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, false, -1)
    }
}

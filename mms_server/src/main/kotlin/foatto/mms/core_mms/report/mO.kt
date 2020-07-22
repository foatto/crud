@file:JvmName("mO")
package foatto.mms.core_mms.report

import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.mAbstractReport
import foatto.mms.core_mms.ObjectSelector
import foatto.sql.CoreAdvancedStatement

class mO : mAbstractReport() {

    private lateinit var os: ObjectSelector

    val columnReportObject: ColumnInt
        get() = os.columnObject

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt( tableName, "id" )

        //----------------------------------------------------------------------------------------------------------------------

        initReportCapAndSignature( aliasConfig, userConfig )

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add( columnID!! )

        //----------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
        os.fillColumns( this, true, true, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, false, -1 )

        //----------------------------------------------------------------------------------------------------------------------

        addCapAndSignatureColumns()
    }
}

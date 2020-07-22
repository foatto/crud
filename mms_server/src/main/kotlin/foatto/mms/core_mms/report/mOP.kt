package foatto.mms.core_mms.report

import foatto.app.CoreSpringController
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.mms.core_mms.MMSFunction
import foatto.mms.core_mms.ObjectSelector
import foatto.sql.CoreAdvancedStatement

class mOP : mP() {

    private lateinit var os: ObjectSelector

    val columnReportObject: ColumnInt
        get() = os.columnObject

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        //--- отдельная обработка перехода от журнала суточных работ/рабочих смен/путёвок/журнала сменных работ
        val arrDT = MMSFunction.getDayShiftWorkParent(stm, zoneId, hmParentData, false)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        initReportPeriod(arrDT)

        initReportCapAndSignature(aliasConfig, userConfig)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID!!)

        //----------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
        os.fillColumns(this, true, true, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, false, -1)

        //----------------------------------------------------------------------------------------------------------------------

        addReportPeriodFormColumns()

        addCapAndSignatureColumns()
    }
}

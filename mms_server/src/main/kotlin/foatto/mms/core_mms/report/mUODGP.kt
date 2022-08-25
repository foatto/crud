package foatto.mms.core_mms.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.mms.core_mms.MMSFunction
import foatto.mms.core_mms.UODGSelector
import foatto.sql.CoreAdvancedStatement

open class mUODGP : mP() {

    lateinit var uodg: UODGSelector
        private set

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        //--- отдельная обработка перехода от журнала суточных работ/рабочих смен/путёвок/журнала сменных работ
        val arrDT = MMSFunction.getDayShiftWorkParent(stm, zoneId, hmParentData, false)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        initReportPeriod(arrDT)

        //----------------------------------------------------------------------------------------------------------------------

        defineOptionsColumns(userConfig)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)

        //----------------------------------------------------------------------------------------------------------------------

        uodg = UODGSelector()
        uodg.fillColumns(application, modelTableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn)

        addReportPeriodFormColumns()

        addOptionsColumns(userConfig)
    }

    open fun defineOptionsColumns(userConfig: UserConfig) {}

    open fun addOptionsColumns(userConfig: UserConfig) {}
}

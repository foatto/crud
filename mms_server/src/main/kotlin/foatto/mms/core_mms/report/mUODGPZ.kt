package foatto.mms.core_mms.report

import foatto.app.CoreSpringController
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.mms.core_mms.MMSFunction
import foatto.mms.core_mms.UODGSelector
import foatto.sql.CoreAdvancedStatement

class mUODGPZ : mP() {

    lateinit var uodg: UODGSelector
        private set

    lateinit var columnReportZone: ColumnInt
        private set

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(
        appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int
    ) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //--- отдельная обработка перехода от журнала суточных работ/рабочих смен/путёвок/журнала сменных работ
        val arrDT = MMSFunction.getDayShiftWorkParent(stm, zoneId, hmParentData, false)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        initReportPeriod(arrDT)

        val columnZoneID = ColumnInt("MMS_zone", "id")
        columnReportZone = ColumnInt(tableName, "zone_id", columnZoneID)
        val columnZoneName = ColumnString("MMS_zone", "name", "Наименование геозоны", STRING_COLUMN_WIDTH)
        val columnZoneDescr = ColumnString("MMS_zone", "descr", "Описание геозоны", STRING_COLUMN_WIDTH)

        columnZoneName.selectorAlias = "mms_zone"
        columnZoneName.addSelectorColumn(columnReportZone, columnZoneID)
        columnZoneName.addSelectorColumn(columnZoneName)
        columnZoneName.addSelectorColumn(columnZoneDescr)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnReportZone)

        uodg = UODGSelector()
        uodg.fillColumns(tableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn)

        addReportPeriodFormColumns()

        alFormColumn.add(columnZoneName)
        alFormColumn.add(columnZoneDescr)

        //----------------------------------------------------------------------------------------------------------------------

        hmParentColumn["mms_zone"] = columnReportZone
    }
}

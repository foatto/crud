package foatto.mms.core_mms.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.mms.core_mms.MMSFunction
import foatto.mms.core_mms.UODGSelector
import foatto.sql.CoreAdvancedStatement

class mLiquidIncWaybill : mP() {

    lateinit var uodg: UODGSelector
        private set

    lateinit var columnTimeType: ColumnComboBox
        private set

    lateinit var columnReportZone: ColumnInt
        private set

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(
        application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //--- отдельная обработка перехода от журнала суточных работ/рабочих смен/путёвок/журнала сменных работ
        val arrDT = MMSFunction.getDayShiftWorkParent(stm, zoneId, hmParentData, false)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        initReportPeriod(arrDT)

        columnTimeType = ColumnComboBox(modelTableName, "waybill_time_range_type", "Используемое время начала/окончания", mWorkShiftCompare.TIME_TYPE_DOC)
        columnTimeType.addChoice(mWorkShiftCompare.TIME_TYPE_DOC, "Заявленное")
        columnTimeType.addChoice(mWorkShiftCompare.TIME_TYPE_FACT, "Фактическое")
        //columnTimeType.addChoice( TIME_TYPE_DAY, "Начало/окончание суток" );
        columnTimeType.isVirtual = true
        columnTimeType.setSavedDefault(userConfig)

        val columnZoneID = ColumnInt("MMS_zone", "id")
        columnReportZone = ColumnInt(modelTableName, "zone_id", columnZoneID)
        val columnZoneName = ColumnString("MMS_zone", "name", "Наименование геозоны", STRING_COLUMN_WIDTH)
        val columnZoneDescr = ColumnString("MMS_zone", "descr", "Описание геозоны", STRING_COLUMN_WIDTH)

        columnZoneName.selectorAlias = "mms_zone"
        columnZoneName.addSelectorColumn(columnReportZone, columnZoneID)
        columnZoneName.addSelectorColumn(columnZoneName)
        columnZoneName.addSelectorColumn(columnZoneDescr)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnReportZone)

        uodg = UODGSelector()
        uodg.fillColumns(application, modelTableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn)

        addReportPeriodFormColumns()

        alFormColumn.add(columnTimeType)
        alFormColumn.add(columnZoneName)
        alFormColumn.add(columnZoneDescr)

        //----------------------------------------------------------------------------------------------------------------------

        hmParentColumn["mms_zone"] = columnReportZone
    }
}

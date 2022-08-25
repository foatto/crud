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

class mObjectZone : mP() {

    companion object {

        val TYPE_DETAIL = 0
        val TYPE_SUMMARY = 1

        val GROUP_BY_OBJECT = 0
        val GROUP_BY_ZONE = 1
    }

    lateinit var uodg: UODGSelector
        private set

    lateinit var columnReportZone: ColumnInt
        private set

    lateinit var columnReportType: ColumnComboBox
        private set
    lateinit var columnReportGroupType: ColumnComboBox
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

        val columnZoneID = ColumnInt("MMS_zone", "id")
        columnReportZone = ColumnInt(modelTableName, "zone_id", columnZoneID)
        val columnZoneName = ColumnString("MMS_zone", "name", "Наименование геозоны", STRING_COLUMN_WIDTH)
        val columnZoneDescr = ColumnString("MMS_zone", "descr", "Описание геозоны", STRING_COLUMN_WIDTH)

        columnZoneName.selectorAlias = "mms_zone"
        columnZoneName.addSelectorColumn(columnReportZone!!, columnZoneID)
        columnZoneName.addSelectorColumn(columnZoneName)
        columnZoneName.addSelectorColumn(columnZoneDescr)

        columnReportType = ColumnComboBox(modelTableName, "object_zone_report_type", "Тип отчёта", TYPE_DETAIL)
        columnReportType.addChoice(TYPE_DETAIL, "Подробный")
        columnReportType.addChoice(TYPE_SUMMARY, "Суммарный")
        columnReportType.isVirtual = true
        columnReportType.setSavedDefault(userConfig)

        columnReportGroupType = ColumnComboBox(modelTableName, "object_zone_group_type", "Группировка", GROUP_BY_OBJECT)
        columnReportGroupType.addChoice(GROUP_BY_OBJECT, "По объектам")
        columnReportGroupType.addChoice(GROUP_BY_ZONE, "По геозонам")
        columnReportGroupType.isVirtual = true
        columnReportGroupType.setSavedDefault(userConfig)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnReportZone)

        uodg = UODGSelector()
        uodg.fillColumns(application, modelTableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn)

        addReportPeriodFormColumns()

        alFormColumn.add(columnZoneName)
        alFormColumn.add(columnZoneDescr)
        alFormColumn.add(columnReportType)
        alFormColumn.add(columnReportGroupType)

        //----------------------------------------------------------------------------------------------------------------------

        hmParentColumn["mms_zone"] = columnReportZone
    }
}

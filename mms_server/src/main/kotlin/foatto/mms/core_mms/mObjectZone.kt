package foatto.mms.core_mms

import foatto.app.CoreSpringController
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.FormColumnVisibleData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mObjectZone : mAbstract() {

    override fun isUseParentUserID(): Boolean {
        return true
    }

    override fun init(
        appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int
    ) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_object_zone"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnZoneID = ColumnInt("MMS_zone", "id")
        val columnZone = ColumnInt(tableName, "zone_id", columnZoneID)
        val columnZoneName = ColumnString("MMS_zone", "name", "Наименование геозоны", STRING_COLUMN_WIDTH)
        columnZoneName.isRequired = true
        val columnZoneDescr = ColumnString("MMS_zone", "descr", "Описание геозоны", STRING_COLUMN_WIDTH)

        columnZoneName.selectorAlias = "mms_zone"
        columnZoneName.addSelectorColumn(columnZone, columnZoneID)
        columnZoneName.addSelectorColumn(columnZoneName)
        columnZoneName.addSelectorColumn(columnZoneDescr)

        val columnZoneType = ColumnComboBox(tableName, "zone_type", "Ограничение", ZoneLimitData.TYPE_LIMIT_SPEED)
        columnZoneType.addChoice(ZoneLimitData.TYPE_LIMIT_SPEED, "Ограничение по скорости")
        columnZoneType.addChoice(ZoneLimitData.TYPE_LIMIT_AREA_BLOCKED, "Нахождение в геозоне запрещено")
        columnZoneType.addChoice(ZoneLimitData.TYPE_LIMIT_AREA_ONLY, "Нахождение вне геозоны запрещено")
        columnZoneType.addChoice(ZoneLimitData.TYPE_LIMIT_PARKING_BLOCKED, "Стоянка в геозоне запрещена")
        columnZoneType.addChoice(ZoneLimitData.TYPE_LIMIT_PARKING_ONLY, "Стоянка вне геозоны запрещена")
        //            //--- заполнение дополнительных ограничений по датчикам
        //            ZoneLimitData.fillZoneLimitComboBox( columnZoneType );

        val columnZoneMaxSpeed = ColumnInt(tableName, "max_speed", "Максимальная скорость [км/ч]", 10, 100)
        columnZoneMaxSpeed.addFormVisible(FormColumnVisibleData(columnZoneType, true, intArrayOf(ZoneLimitData.TYPE_LIMIT_SPEED)))

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnZone)

        addTableColumn(columnZoneName)
        addTableColumn(columnZoneDescr)
        addTableColumn(columnZoneType)

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnZone)

        //----------------------------------------------------------------------------------------------------------------------

        val os = ObjectSelector()
        os.fillColumns(this, true, true, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, hmParentData["mms_object"] != null, -1)

        //----------------------------------------------------------------------------------------------------------------------

        alFormColumn.add(columnZoneName)
        alFormColumn.add(columnZoneDescr)
        alFormColumn.add(columnZoneType)
        alFormColumn.add(columnZoneMaxSpeed)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnZoneName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["mms_zone"] = columnZone
    }
}

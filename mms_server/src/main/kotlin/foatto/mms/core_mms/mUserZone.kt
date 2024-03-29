package foatto.mms.core_mms

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstractUserSelector
import foatto.sql.CoreAdvancedConnection

class mUserZone : mAbstractUserSelector() {

    override fun init(
        application: iApplication,
        aConn: CoreAdvancedConnection,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_user_zone"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserName = addUserSelector(userConfig)

        //----------------------------------------------------------------------------------------------------------------------

        val columnZoneID = ColumnInt("MMS_zone", "id")
        val columnZone = ColumnInt(modelTableName, "zone_id", columnZoneID)
        val columnZoneDescr = ColumnString("MMS_zone", "descr", "Описание геозоны", STRING_COLUMN_WIDTH)

        val columnZoneName = ColumnString("MMS_zone", "name", "Наименование геозоны", STRING_COLUMN_WIDTH).apply {
            isRequired = true
            selectorAlias = "mms_zone"
            addSelectorColumn(columnZone, columnZoneID)
            addSelectorColumn(this)
            addSelectorColumn(columnZoneDescr)
        }

        val columnZoneType = ColumnComboBox(modelTableName, "zone_type", "Ограничение", ZoneLimitData.TYPE_LIMIT_SPEED).apply {
            addChoice(ZoneLimitData.TYPE_LIMIT_SPEED, "Ограничение по скорости")
            addChoice(ZoneLimitData.TYPE_LIMIT_AREA_BLOCKED, "Нахождение в геозоне запрещено")
            addChoice(ZoneLimitData.TYPE_LIMIT_AREA_ONLY, "Нахождение вне геозоны запрещено")
            addChoice(ZoneLimitData.TYPE_LIMIT_PARKING_BLOCKED, "Стоянка в геозоне запрещена")
            addChoice(ZoneLimitData.TYPE_LIMIT_PARKING_ONLY, "Стоянка вне геозоны запрещена")
            //            //--- заполнение дополнительных ограничений по датчикам
            //            ZoneLimitData.fillZoneLimitComboBox( columnZoneType );
        }
        val columnZoneMaxSpeed = ColumnInt(modelTableName, "max_speed", "Максимальная скорость [км/ч]", 10, 100).apply {
            addFormVisible(columnZoneType, true, setOf(ZoneLimitData.TYPE_LIMIT_SPEED))
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnUser!!)
        alTableHiddenColumn.add(columnZone)

        addTableColumn(columnZoneName)
        addTableColumn(columnZoneDescr)
        addTableColumn(columnZoneType)

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnUser!!)
        alFormHiddenColumn.add(columnZone)

        alFormColumn.add(columnUserName)
        alFormColumn.add(columnZoneName)
        alFormColumn.add(columnZoneDescr)
        alFormColumn.add(columnZoneType)
        alFormColumn.add(columnZoneMaxSpeed)

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnZoneName, true)

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!
        hmParentColumn["mms_zone"] = columnZone
    }
}

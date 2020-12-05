package foatto.mms.core_mms

import foatto.app.CoreSpringController
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mUserZone : mAbstract() {

    override fun init(
        appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int
    ) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_user_zone"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        val columnUserID = ColumnInt("SYSTEM_users", "id")
        columnUser = ColumnInt(tableName, "user_id", columnUserID, userConfig.userID)
        val columnUserName = ColumnString("SYSTEM_users", "full_name", "Владелец геозоны", STRING_COLUMN_WIDTH).apply {
            if (userConfig.isAdmin) {
                //columnUserName.setRequired( true ); - может быть ничья/общая
                selectorAlias = "system_user_people"
                addSelectorColumn(columnUser!!, columnUserID)
                addSelectorColumn(this)
            }
        }

        //----------------------------------------------------------------------------------------------------------------------

        val columnZoneID = ColumnInt("MMS_zone", "id")
        val columnZone = ColumnInt(tableName, "zone_id", columnZoneID)
        val columnZoneDescr = ColumnString("MMS_zone", "descr", "Описание геозоны", STRING_COLUMN_WIDTH)

        val columnZoneName = ColumnString("MMS_zone", "name", "Наименование геозоны", STRING_COLUMN_WIDTH).apply {
            isRequired = true
            selectorAlias = "mms_zone"
            addSelectorColumn(columnZone, columnZoneID)
            addSelectorColumn(this)
            addSelectorColumn(columnZoneDescr)
        }

        val columnZoneType = ColumnComboBox(tableName, "zone_type", "Ограничение", ZoneLimitData.TYPE_LIMIT_SPEED).apply {
            addChoice(ZoneLimitData.TYPE_LIMIT_SPEED, "Ограничение по скорости")
            addChoice(ZoneLimitData.TYPE_LIMIT_AREA_BLOCKED, "Нахождение в геозоне запрещено")
            addChoice(ZoneLimitData.TYPE_LIMIT_AREA_ONLY, "Нахождение вне геозоны запрещено")
            addChoice(ZoneLimitData.TYPE_LIMIT_PARKING_BLOCKED, "Стоянка в геозоне запрещена")
            addChoice(ZoneLimitData.TYPE_LIMIT_PARKING_ONLY, "Стоянка вне геозоны запрещена")
            //            //--- заполнение дополнительных ограничений по датчикам
            //            ZoneLimitData.fillZoneLimitComboBox( columnZoneType );
        }
        val columnZoneMaxSpeed = ColumnInt(tableName, "max_speed", "Максимальная скорость [км/ч]", 10, 100).apply {
            addFormVisible(columnZoneType, true, setOf(ZoneLimitData.TYPE_LIMIT_SPEED))
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnUser!!)
        alTableHiddenColumn.add(columnZone)

        addTableColumn(columnZoneName)
        addTableColumn(columnZoneDescr)
        addTableColumn(columnZoneType)

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnUser!!)
        alFormHiddenColumn.add(columnZone)

        alFormColumn.add(columnUserName)
        alFormColumn.add(columnZoneName)
        alFormColumn.add(columnZoneDescr)
        alFormColumn.add(columnZoneType)
        alFormColumn.add(columnZoneMaxSpeed)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnZoneName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!
        hmParentColumn["mms_zone"] = columnZone
    }
}

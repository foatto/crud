package foatto.mms.core_mms

import foatto.app.CoreSpringController
import foatto.core.link.AppAction
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mZone : mAbstract() {

    lateinit var columnZoneName: ColumnString
        private set
    lateinit var columnZoneDescr: ColumnString
        private set

    override fun init(
        appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int
    ) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_zone"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        val columnUserID = ColumnInt("SYSTEM_users", "id")
        columnUser = ColumnInt(tableName, "user_id", columnUserID, userConfig.userID)
        val columnUserName = ColumnString("SYSTEM_users", "full_name", "Владелец геозоны", STRING_COLUMN_WIDTH)
        if(userConfig.isAdmin) {
            //columnUserName.setRequired( true ); - может быть ничья/общая
            columnUserName.selectorAlias = "system_user_people"
            columnUserName.addSelectorColumn(columnUser!!, columnUserID)
            columnUserName.addSelectorColumn(columnUserName)
        }

        //----------------------------------------------------------------------------------------------------------------------

        columnZoneName = ColumnString(tableName, "name", "Наименование геозоны", STRING_COLUMN_WIDTH)
        columnZoneName.isRequired = true
        //columnZoneName.setUnique( true ); !!! у разных корпоративных клиентов могут быть совпадающие значения
        columnZoneDescr = ColumnString(tableName, "descr", "Описание геозоны", STRING_COLUMN_WIDTH)
        val columnZoneOuterID = ColumnString(tableName, "outer_id", "Внешний идентификатор", STRING_COLUMN_WIDTH)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnUser!!)

        addTableColumn(columnZoneName)
        addTableColumn(columnZoneDescr)
        addTableColumn(columnZoneOuterID)

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnUser!!)

        alFormColumn.add(columnUserName)
        alFormColumn.add(columnZoneName)
        alFormColumn.add(columnZoneDescr)
        alFormColumn.add(columnZoneOuterID)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnZoneName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("mms_show_zone", columnID!!, AppAction.FORM, true))
        alChildData.add(ChildData("mms_user_zone", columnID!!))
        alChildData.add(ChildData("mms_object_zone", columnID!!))
        //alChildData.add( new ChildData( "mms_route_zone" , columnID ) );
        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_inc", columnID!!, AppAction.FORM, true))
        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_dec", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_speed", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_parking", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_object_zone", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_weight", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_turn", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_pressure", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_temperature", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_voltage", columnID!!, AppAction.FORM))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("MMS_user_zone", "zone_id"))
        alDependData.add(DependData("MMS_object_zone", "zone_id"))
        //alDependData.add( new DependData( "MMS_route_zone", "zone_id" ) );
    }

}

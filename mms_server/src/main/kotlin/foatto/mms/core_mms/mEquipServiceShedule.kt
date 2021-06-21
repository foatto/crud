package foatto.mms.core_mms

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mEquipServiceShedule : mAbstract() {

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val equipID = hmParentData["mms_equip"]

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_equip_service_shedule"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnEquip = ColumnInt(tableName, "equip_id", equipID)

        val columnServiceName = ColumnString(tableName, "name", "Наименование", STRING_COLUMN_WIDTH)
        val columnServicePeriod = ColumnDouble(tableName, "period", "Периодичность обслуживания [мото-час]", 10, 1, 0.0)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)
        alTableHiddenColumn.add(columnEquip)

        addTableColumn(columnServiceName)
        addTableColumn(columnServicePeriod)

        alFormHiddenColumn.add(columnID)
        alFormHiddenColumn.add(columnEquip)

        alFormColumn.add(columnServiceName)
        alFormColumn.add(columnServicePeriod)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnServiceName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["mms_equip"] = columnEquip
    }
}

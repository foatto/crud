package foatto.mms.core_mms

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedConnection

class mEquipServiceShedule : mAbstract() {

    override fun init(application: iApplication, aConn: CoreAdvancedConnection, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val equipID = hmParentData["mms_equip"]

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_equip_service_shedule"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnEquip = ColumnInt(modelTableName, "equip_id", equipID)

        val columnServiceName = ColumnString(modelTableName, "name", "Наименование", STRING_COLUMN_WIDTH)
        val columnServicePeriod = ColumnDouble(modelTableName, "period", "Периодичность обслуживания [мото-час]", 10, 1, 0.0)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnEquip)

        addTableColumn(columnServiceName)
        addTableColumn(columnServicePeriod)

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnEquip)

        alFormColumn.add(columnServiceName)
        alFormColumn.add(columnServicePeriod)

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnServiceName, true)

        //----------------------------------------------------------------------------------------

        hmParentColumn["mms_equip"] = columnEquip
    }
}

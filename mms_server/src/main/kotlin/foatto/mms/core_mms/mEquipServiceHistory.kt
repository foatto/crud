package foatto.mms.core_mms

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mEquipServiceHistory : mAbstract() {

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val sensorID = hmParentData["mms_equip"]

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_equip_service_history"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnEquip = ColumnInt(modelTableName, "equip_id", sensorID)

        val columnServiceName = ColumnComboBox(modelTableName, "shedule_id", "Наименование", 0)
        if(sensorID != null) {
            val rs = stm.executeQuery(" SELECT id , name FROM MMS_equip_service_shedule WHERE equip_id = $sensorID ORDER BY name ")
            while(rs.next()) columnServiceName.addChoice(rs.getInt(1), rs.getString(2))
            rs.close()
        }

        val columnServiceDate = ColumnDate3Int(modelTableName, "ye", "mo", "da", "Дата")
        val columnServiceWork = ColumnDouble(modelTableName, "work_hour", "Наработка [мото-час]", 10, 1, 0.0)
        val columnServiceDescr = ColumnString(modelTableName, "descr", "Примечания", STRING_COLUMN_WIDTH)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnEquip)

        addTableColumn(columnServiceName)
        addTableColumn(columnServiceDate)
        addTableColumn(columnServiceWork)
        addTableColumn(columnServiceDescr)

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnEquip)

        alFormColumn.add(columnServiceName)
        alFormColumn.add(columnServiceDate)
        alFormColumn.add(columnServiceWork)
        alFormColumn.add(columnServiceDescr)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnServiceDate)
        alTableSortDirect.add("DESC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["mms_equip"] = columnEquip
    }
}

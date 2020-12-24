package foatto.mms.core_mms.sensor

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mSensorCalibration : mAbstract() {

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_sensor_calibration"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnSensor = ColumnInt(tableName, "sensor_id", hmParentData["mms_sensor"]!!)

        val columnValueSensor = ColumnDouble(tableName, "value_sensor", "Значение датчика", 10, -1, 0.0)
        val columnValueData = ColumnDouble(tableName, "value_data", "Значение измеряемой величины", 10, -1, 0.0)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnSensor)

        addTableColumn(columnValueSensor)
        addTableColumn(columnValueData)

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnSensor)

        alFormColumn.add(columnValueSensor)
        alFormColumn.add(columnValueData)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnValueSensor)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["mms_sensor"] = columnSensor
    }
}

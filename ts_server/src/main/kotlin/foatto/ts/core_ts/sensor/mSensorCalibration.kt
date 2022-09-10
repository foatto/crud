package foatto.ts.core_ts.sensor

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedConnection

class mSensorCalibration : mAbstract() {

    override fun init(application: iApplication, aConn: CoreAdvancedConnection, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "TS_sensor_calibration"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnSensor = ColumnInt(modelTableName, "sensor_id", hmParentData["ts_sensor"])

        val columnValueSensor = ColumnDouble(modelTableName, "value_sensor", "Значение датчика", 10, -1, 0.0)
        val columnValueData = ColumnDouble(modelTableName, "value_data", "Значение измеряемой величины", 10, -1, 0.0)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnSensor)

        addTableColumn(columnValueSensor)
        addTableColumn(columnValueData)

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnSensor)

        alFormColumn.add(columnValueSensor)
        alFormColumn.add(columnValueData)

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnValueSensor, true)

        //----------------------------------------------------------------------------------------

        hmParentColumn["ts_sensor"] = columnSensor
    }
}

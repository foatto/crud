package foatto.mms.core_mms

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.sql.CoreAdvancedStatement

class mWorkShiftData : mAbstract() {

    lateinit var columnDataType: ColumnInt
        private set

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val isWorkData = aliasConfig.alias == "mms_work_shift_work"
        val isLiquidData = aliasConfig.alias == "mms_work_shift_liquid"

        val shiftID: Int? = hmParentData["mms_work_shift"] ?: hmParentData["mms_waybill"]

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_work_shift_data"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnShift = ColumnInt(modelTableName, "shift_id", shiftID)

        columnDataType = ColumnInt(modelTableName, "data_type", if (isWorkData) SensorConfig.SENSOR_WORK else if (isLiquidData) SensorConfig.SENSOR_LIQUID_USING else 0)

        val columnName = ColumnString(modelTableName, "name", "", STRING_COLUMN_WIDTH)

        val columnDescr = ColumnString(modelTableName, "descr", if (isWorkData) "Оборудование" else if (isLiquidData) "Топливо" else "", STRING_COLUMN_WIDTH)
        if (shiftID != null) {
            var objectId = 0
            var rs = stm.executeQuery(" SELECT object_id FROM MMS_work_shift WHERE id = $shiftID ")
            if (rs.next()) objectId = rs.getInt(1)
            rs.close()

            if (objectId != 0) {
                if (isWorkData) {
                    //--- DISTINCT deliberately will not be set,
                    //--- let repetitions be detected in equipment names
                    rs = stm.executeQuery(
                        " SELECT descr FROM MMS_sensor WHERE sensor_type = ${SensorConfig.SENSOR_WORK} AND object_id = $objectId ORDER BY descr "
                    )
                    while (rs.next()) columnDescr.addCombo(rs.getString(1))
                    rs.close()
                }
                if (isLiquidData) {
                    //--- set DISTINCT to remove duplication of the same fuel names
                    rs = stm.executeQuery(" SELECT DISTINCT liquid_name FROM MMS_sensor WHERE object_id = $objectId ORDER BY liquid_name ")
                    while (rs.next()) columnDescr.addCombo(rs.getString(1))
                    rs.close()
                }
            }
        }

        val columnValue = ColumnDouble(
            modelTableName, "data_value",
            if (isWorkData) "Наработка [мото-час]" else if (isLiquidData) "Расход" else "", 10, 1, 0.0
        )

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)
        alTableHiddenColumn.add(columnShift)
        alTableHiddenColumn.add(columnDataType)
        alTableHiddenColumn.add(columnName)

        addTableColumn(columnDescr)
        addTableColumn(columnValue)

        alFormHiddenColumn.add(columnID)
        alFormHiddenColumn.add(columnShift)
        alFormHiddenColumn.add(columnDataType)
        alFormHiddenColumn.add(columnName)

        alFormColumn.add(columnDescr)
        alFormColumn.add(columnValue)

        //----------------------------------------------------------------------------------------------------------------------

        alTableSortColumn.add(columnDescr)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["mms_work_shift"] = columnShift
        hmParentColumn["mms_waybill"] = columnShift
    }
}

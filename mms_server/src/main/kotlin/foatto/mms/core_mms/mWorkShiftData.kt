package foatto.mms.core_mms

import foatto.app.CoreSpringController
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.mms.core_mms.sensor.SensorConfig
import foatto.sql.CoreAdvancedStatement

class mWorkShiftData : mAbstract() {

    lateinit var columnDataType: ColumnInt
        private set

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val isWorkData = aliasConfig.alias == "mms_work_shift_work"
        val isLiquidData = aliasConfig.alias == "mms_work_shift_liquid"

        val shiftID: Int? = hmParentData["mms_work_shift"] ?: hmParentData["mms_waybill"]

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_work_shift_data"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnShift = ColumnInt(tableName, "shift_id", shiftID)

        columnDataType = ColumnInt(tableName, "data_type", if(isWorkData) SensorConfig.SENSOR_WORK else if(isLiquidData) SensorConfig.SENSOR_VOLUME_FLOW else 0)

        val columnName = ColumnString(tableName, "name", "", STRING_COLUMN_WIDTH)

        val columnDescr = ColumnString(tableName, "descr", if(isWorkData) "Оборудование" else if(isLiquidData) "Топливо" else "", STRING_COLUMN_WIDTH)
        if(shiftID != null) {
            var objectID = 0
            var rs = stm.executeQuery(" SELECT object_id FROM MMS_work_shift WHERE id = $shiftID ")
            if(rs.next()) objectID = rs.getInt(1)
            rs.close()

            if(objectID != 0) {
                if(isWorkData) {
                    //--- DISTINCT намеренно ставить не буду,
                    //--- пусть обнаруживаются повторы в наименованиях оборудования
                    rs = stm.executeQuery(
                        " SELECT descr FROM MMS_sensor WHERE sensor_type = ${SensorConfig.SENSOR_WORK} AND object_id = $objectID ORDER BY descr "
                    )
                    while(rs.next()) columnDescr.addCombo(rs.getString(1))
                    rs.close()
                }
                if(isLiquidData) {
                    //--- ставим DISTINCT чтобы убрать дубляж одинаковых наименований топлива
                    rs = stm.executeQuery(" SELECT DISTINCT liquid_name FROM MMS_sensor WHERE object_id = $objectID ORDER BY liquid_name ")
                    while(rs.next()) columnDescr.addCombo(rs.getString(1))
                    rs.close()
                }
            }
        }

        val columnValue = ColumnDouble(
            tableName, "data_value",
            if(isWorkData) "Наработка [мото-час]" else if(isLiquidData) "Расход [л]" else "", 10, 1, 0.0
        )

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnShift)
        alTableHiddenColumn.add(columnDataType)
        alTableHiddenColumn.add(columnName)

        addTableColumn(columnDescr)
        addTableColumn(columnValue)

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnShift)
        alFormHiddenColumn.add(columnDataType)
        alFormHiddenColumn.add(columnName)

        alFormColumn.add(columnDescr)
        alFormColumn.add(columnValue)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnDescr)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["mms_work_shift"] = columnShift
        hmParentColumn["mms_waybill"] = columnShift
    }
}

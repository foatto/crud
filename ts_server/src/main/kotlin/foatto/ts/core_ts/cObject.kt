package foatto.ts.core_ts

import foatto.core.link.TableCell
import foatto.core.link.TableCellBackColorType
import foatto.core.link.TableCellForeColorType
import foatto.core.link.TableResponse
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataDateTimeInt
import foatto.core_server.app.server.data.DataDouble
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.ts.core_ts.calc.AbstractObjectStateCalc
import foatto.ts.core_ts.calc.ObjectState
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigState
import foatto.ts.iTSApplication

class cObject : cStandart() {

    private val hmObjectConfigCache = mutableMapOf<Int,ObjectConfig>()
    private val hmObjectStateCache = mutableMapOf<Int,ObjectState>()

    companion object {
        const val PERM_REMOTE_CONTROL = "remote_control"
    }

    override fun definePermission() {
        super.definePermission()

        //--- права доступа на ( дистанционное ) управление объектом
        alPermission.add(Pair(PERM_REMOTE_CONTROL, "20 Remote Control"))
    }

    override fun getTable(hmOut: MutableMap<String, Any>): TableResponse {
        hmObjectConfigCache.clear()
        hmObjectStateCache.clear()

        return super.getTable(hmOut)
    }

    override fun getTableColumnStyle(rowNo: Int, isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(rowNo, isNewRow, hmColumnData, column, tci)

        val id = (hmColumnData[model.columnID] as DataInt).intValue
        val objectConfig = hmObjectConfigCache.getOrPut(id) { (application as iTSApplication).getObjectConfig(userConfig, id) }
        val objectState = hmObjectStateCache.getOrPut(id) { ObjectState.getState(stm, objectConfig) }

        val md = model as mObject
        when (column) {
            md.columnState -> {
                objectState.tmStateValue.values.firstOrNull()?.let { stateCode ->
                    SensorConfigState.hmStateInfo[stateCode]?.brightColor
                }?.let { brightColor ->
                    tci.backColorType = TableCellBackColorType.DEFINED
                    tci.backColor = brightColor
                }
            }
            md.columnLastDateTime -> {
                tci.foreColorType = TableCellForeColorType.DEFINED

//            if( ( hmColumnData[ md.columnDisabled ] as DataBoolean).value ) {
//                tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
//            }
//            else {
                val lastDataTime = objectState.lastDateTime ?: 0

                //--- нет данных больше суток - критично
                if (getCurrentTimeInt() - lastDataTime > 1 * 24 * 60 * 60) {
                    tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
                }
                //--- нет данных больше часа - предупреждение + начинается оповещение по e-mail
                else if (getCurrentTimeInt() - lastDataTime > 1 * 60 * 60) {
                    tci.foreColor = TABLE_CELL_FORE_COLOR_WARNING
                }
                //--- всё нормально
                else {
                    tci.foreColor = TABLE_CELL_FORE_COLOR_NORMAL
                }
            }
        }
    }

    override fun generateColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {
        super.generateColumnDataAfterFilter(hmColumnData)

        generateColumnData((hmColumnData[model.columnID] as DataInt).intValue, hmColumnData)
    }

    override fun generateFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        super.generateFormColumnData(id, hmColumnData)

        generateColumnData(id, hmColumnData)
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)
        createDataTable(id)
        return postURL
    }

    override fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
        super.postDelete(id, hmColumnData)
        deleteDataTable(id)

        stm.executeUpdate(" DELETE FROM TS_sensor_calibration WHERE sensor_id IN ( SELECT id FROM TS_sensor WHERE object_id = $id ) ")
        stm.executeUpdate(" DELETE FROM TS_sensor WHERE object_id = $id ")
    }

    private fun createDataTable(id: Int) {
        stm.executeUpdate(" CREATE TABLE TS_data_$id ( ontime ${stm.dialect.integerFieldTypeName} NOT NULL, sensor_data ${stm.dialect.hexFieldTypeName} ) ")
        stm.executeUpdate(stm.dialect.createClusteredIndex + " TS_data_${id}_ontime ON TS_data_$id ( ontime ) ")
    }

    private fun deleteDataTable(id: Int) {
        stm.executeUpdate(" DROP TABLE TS_data_$id ")
    }

    private fun generateColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        val m = model as mObject

        val objectConfig = hmObjectConfigCache.getOrPut(id) { (application as iTSApplication).getObjectConfig(userConfig, id) }
        val objectState = hmObjectStateCache.getOrPut(id) { ObjectState.getState(stm, objectConfig) }

        (hmColumnData[m.columnLastDateTime] as DataDateTimeInt).setDateTime(objectState.lastDateTime ?: 0)

        (hmColumnData[m.columnState] as DataString).text = objectState.tmStateValue.values.firstOrNull()?.let { stateCode ->
            SensorConfigState.hmStateInfo[stateCode]?.descr ?: "(неизвестный код состояния: $stateCode)"
        } ?: "(нет данных)"

        (hmColumnData[m.columnDepth] as DataDouble).doubleValue = objectState.tmDepthValue.values.firstOrNull() ?: 0.0
        (hmColumnData[m.columnSpeed] as DataDouble).doubleValue = objectState.tmSpeedValue.values.firstOrNull() ?: 0.0
        (hmColumnData[m.columnLoad] as DataDouble).doubleValue = objectState.tmLoadValue.values.firstOrNull() ?: 0.0
    }


}

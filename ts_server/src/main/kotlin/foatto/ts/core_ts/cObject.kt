package foatto.ts.core_ts

import foatto.core.link.TableCell
import foatto.core.link.TableCellBackColorType
import foatto.core.link.TableCellForeColorType
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataDateTimeInt
import foatto.core_server.app.server.data.DataDouble
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.ts.core_ts.calc.AbstractObjectStateCalc
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue
import foatto.ts.core_ts.sensor.config.SensorConfigState
import foatto.ts.iTSApplication

class cObject : cStandart() {

    companion object {
        const val PERM_REMOTE_CONTROL = "remote_control"
    }

    override fun definePermission() {
        super.definePermission()

        //--- права доступа на ( дистанционное ) управление объектом
        alPermission.add(Pair(PERM_REMOTE_CONTROL, "20 Remote Control"))
    }

    override fun getTableColumnStyle(rowNo: Int, isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(rowNo, isNewRow, hmColumnData, column, tci)

        val md = model as mObject
        when (column) {
            md.columnState -> {
                tci.backColorType = TableCellBackColorType.DEFINED

                val id = (hmColumnData[model.columnID] as DataInt).intValue
                val oc = (application as iTSApplication).getObjectConfig(userConfig, id)
                val lastDataTime = getLastDateTime(id)

                lastDataTime?.let {
                    val rs = stm.executeQuery(" SELECT sensor_data FROM TS_data_${id} WHERE ontime = $lastDataTime")
                    if (rs.next()) {
                        val bb = rs.getByteBuffer(1)

                        oc.hmSensorConfig[SensorConfig.SENSOR_STATE]?.get(0)?.let { _ ->
                            val stateCode = AbstractObjectStateCalc.getSensorData(0, bb)?.toInt()
                            SensorConfigState.hmStateInfo[stateCode]?.backColor?.let { backColor ->
                                tci.backColor = backColor
                            } ?: {
                                tci.backColorType = TableCellBackColorType.DEFAULT  // return default background color
                            }
                        }
                    }
                }
            }
            md.columnLastDateTime -> {
                tci.foreColorType = TableCellForeColorType.DEFINED

//            if( ( hmColumnData[ md.columnDisabled ] as DataBoolean).value ) {
//                tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
//            }
//            else {
                val id = (hmColumnData[model.columnID] as DataInt).intValue
                val lastDataTime = getLastDateTime(id) ?: 0

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

        val oc = (application as iTSApplication).getObjectConfig(userConfig, id)
        val lastDataTime = getLastDateTime(id)

        (hmColumnData[m.columnLastDateTime] as DataDateTimeInt).setDateTime(lastDataTime ?: 0)

        lastDataTime?.let {
            val rs = stm.executeQuery(" SELECT sensor_data FROM TS_data_${id} WHERE ontime = $lastDataTime")
            if (rs.next()) {
                val bb = rs.getByteBuffer(1)

                oc.hmSensorConfig[SensorConfig.SENSOR_STATE]?.entries?.firstOrNull()?.let { (portNum, _) ->
                    val stateCode = AbstractObjectStateCalc.getSensorData(portNum, bb)?.toInt()
                    (hmColumnData[m.columnState] as DataString).text = SensorConfigState.hmStateInfo[stateCode]?.descr ?: "(неизвестный код состояния: $stateCode)"
                }

                oc.hmSensorConfig[SensorConfig.SENSOR_DEPTH]?.entries?.firstOrNull()?.let { (portNum, sc) ->
                    val scDepth = sc as SensorConfigAnalogue
                    (hmColumnData[m.columnDepth] as DataDouble).doubleValue = AbstractObjectStateCalc.getSensorValue(
                        alValueSensor = scDepth.alValueSensor,
                        alValueData = scDepth.alValueData,
                        sensorValue = AbstractObjectStateCalc.getSensorData(portNum, bb) as Double
                    )
                }

                oc.hmSensorConfig[SensorConfig.SENSOR_SPEED]?.entries?.firstOrNull()?.let { (portNum, sc) ->
                    val scSpeed = sc as SensorConfigAnalogue
                    (hmColumnData[m.columnSpeed] as DataDouble).doubleValue = AbstractObjectStateCalc.getSensorValue(
                        alValueSensor = scSpeed.alValueSensor,
                        alValueData = scSpeed.alValueData,
                        sensorValue = AbstractObjectStateCalc.getSensorData(portNum, bb) as Double
                    )
                }

                oc.hmSensorConfig[SensorConfig.SENSOR_LOAD]?.entries?.firstOrNull()?.let { (portNum, sc) ->
                    val scLoad = sc as SensorConfigAnalogue
                    (hmColumnData[m.columnLoad] as DataDouble).doubleValue = AbstractObjectStateCalc.getSensorValue(
                        alValueSensor = scLoad.alValueSensor,
                        alValueData = scLoad.alValueData,
                        sensorValue = AbstractObjectStateCalc.getSensorData(portNum, bb) as Double
                    )
                }
            }
            rs.close()
        }
    }

    private fun getLastDateTime(id: Int): Int? {
        val rs = stm.executeQuery(" SELECT MAX(ontime) FROM TS_data_${id} ")
        val lastDateTime = if (rs.next()) {
            rs.getInt(1)
        } else {
            null
        }
        rs.close()

        return lastDateTime
    }

}

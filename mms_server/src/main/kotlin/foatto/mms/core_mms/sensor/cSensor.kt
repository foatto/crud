package foatto.mms.core_mms.sensor

import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.mms.core_mms.cMMSOneObjectParent
import foatto.mms.core_mms.sensor.config.SensorConfig

class cSensor : cMMSOneObjectParent() {

    //--- "оборудование" нельзя добавить, оно является другим отображением "датчика работы оборудования"
    override fun isAddEnabled(): Boolean = aliasConfig.alias != "mms_equip"

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {
        var s = super.addSQLWhere(hsTableRenameList)
        if (aliasConfig.alias == "mms_equip") {
            s += " AND ${renameTableName(hsTableRenameList, model.tableName)}." +
                "${(model as mSensor).columnSensorType.getFieldName()} = ${SensorConfig.SENSOR_WORK} "
        }
        return s
    }

    override fun generateFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        if (id != 0) {
            val ms = model as mSensor

            val sb = StringBuilder()
            val rs = stm.executeQuery(" SELECT value_sensor , value_data FROM MMS_sensor_calibration WHERE sensor_id = $id ORDER BY value_sensor ")
            while (rs.next()) {
                sb.append(rs.getDouble(1)).append(" = ").append(rs.getDouble(2)).append('\n')
            }
            rs.close()

            (hmColumnData[ms.columnCalibrationText] as DataString).text = sb.toString()
        }
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        saveCalibration(id, hmColumnData)

        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        saveCalibration(id, hmColumnData)

        return postURL
    }

    private fun saveCalibration(id: Int, hmColumnData: Map<iColumn, iData>) {
        val ms = model as mSensor

        val calibration = (hmColumnData[ms.columnCalibrationText] as DataString).text
        //--- очистка предыдущей тарировки
        stm.executeUpdate(" DELETE FROM MMS_sensor_calibration WHERE sensor_id = $id ")
        calibration.split('\n').forEach {
            val alSensorData = it.split('=')
            if (alSensorData.size == 2) {
                val sensorValue = alSensorData[0].trim().toDoubleOrNull()
                val dataValue = alSensorData[1].trim().toDoubleOrNull()

                if (sensorValue != null && dataValue != null) {
                    stm.executeUpdate(
                        " INSERT INTO MMS_sensor_calibration ( id , sensor_id , value_sensor , value_data ) VALUES ( +" +
                            stm.getNextID("MMS_sensor_calibration", "id") +
                            " , $id , $sensorValue , $dataValue ) "
                    )
                }
            }
        }
    }

}

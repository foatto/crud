package foatto.ts.core_ts.sensor

import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.ts.core_ts.cTSOneObjectParent

class cSensor : cTSOneObjectParent() {

    override fun getCalculatedFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        if (id != 0) {
            val ms = model as mSensor

            val sb = StringBuilder()
            val rs = conn.executeQuery(" SELECT value_sensor , value_data FROM TS_sensor_calibration WHERE sensor_id = $id ORDER BY value_sensor ")
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
        conn.executeUpdate(" DELETE FROM TS_sensor_calibration WHERE sensor_id = $id ")
        calibration.split('\n').forEach {
            val alSensorData = it.split('=')
            if (alSensorData.size == 2) {
                val sensorValue = alSensorData[0].trim().toDoubleOrNull()
                val dataValue = alSensorData[1].trim().toDoubleOrNull()

                if (sensorValue != null && dataValue != null) {
                    conn.executeUpdate(
                        " INSERT INTO TS_sensor_calibration ( id , sensor_id , value_sensor , value_data ) VALUES ( +" +
                            conn.getNextIntId("TS_sensor_calibration", "id") +
                            " , $id , $sensorValue , $dataValue ) "
                    )
                }
            }
        }
    }

}


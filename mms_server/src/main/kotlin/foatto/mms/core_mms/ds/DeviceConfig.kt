package foatto.mms.core_mms.ds

import foatto.core.app.UP_TIME_OFFSET
import foatto.core.util.getZoneId
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigGeo
import foatto.sql.CoreAdvancedConnection
import java.time.ZoneId

class DeviceConfig {

    var deviceId = 0
    var objectId = 0
    var userId = 0
    var isAutoWorkShift = false
    var index = 0

    var speedRoundRule = SensorConfigGeo.SPEED_ROUND_RULE_STANDART

    lateinit var zoneId: ZoneId

    companion object {

        fun getDeviceConfig(conn: CoreAdvancedConnection, serialNo: String): DeviceConfig? {
            var dc: DeviceConfig? = null

            var rs = conn.executeQuery(
                """
                    SELECT MMS_device.id , MMS_object.id , MMS_object.user_id , MMS_object.is_auto_work_shift , MMS_device.device_index  
                    FROM MMS_object , MMS_device 
                    WHERE MMS_object.id = MMS_device.object_id 
                    AND MMS_device.serial_no = '$serialNo'
                """
            )

            if (rs.next()) {
                dc = DeviceConfig().apply {
                    deviceId = rs.getInt(1)
                    objectId = rs.getInt(2)
                    userId = rs.getInt(3)
                    isAutoWorkShift = rs.getInt(4) != 0
                    index = rs.getInt(5)
                }
            }
            rs.close()

            if (dc != null) {
                rs = conn.executeQuery(" SELECT speed_round_rule FROM MMS_sensor WHERE object_id = ${dc.objectId} AND sensor_type = ${SensorConfig.SENSOR_GEO}")
                dc.speedRoundRule = if (rs.next()) {
                    rs.getInt(1)
                } else {
                    SensorConfigGeo.SPEED_ROUND_RULE_STANDART
                }
                rs.close()

                rs = conn.executeQuery(" SELECT property_value FROM SYSTEM_user_property WHERE user_id = ${dc.userId} AND property_name = '$UP_TIME_OFFSET' ")
                dc.zoneId = if (rs.next()) {
                    getZoneId(rs.getString(1).toInt())
                } else {
                    ZoneId.systemDefault()
                }
                rs.close()
            }

            return dc
        }
    }

}

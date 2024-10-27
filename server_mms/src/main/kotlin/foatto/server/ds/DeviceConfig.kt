package foatto.server.ds

import foatto.core.util.getTimeZone
import foatto.server.model.SensorConfig
import foatto.server.model.SensorConfigGeo
import foatto.server.sql.CoreAdvancedConnection
import kotlinx.datetime.TimeZone

class DeviceConfig(
    val deviceId: Int,
    val objectId: Int,
    val userId: Int,
    var isAutoWorkShift: Boolean,
    val deviceIndex: Int,
) {
    var speedRoundRule: Int = SensorConfigGeo.SPEED_ROUND_RULE_STANDART

    lateinit var timeZone: TimeZone

    companion object {

        fun getDeviceConfig(conn: CoreAdvancedConnection, serialNo: String): DeviceConfig? {
            var rs = conn.executeQuery(
                """
                    SELECT MMS_device.id , MMS_object.id , MMS_object.user_id , MMS_object.is_auto_work_shift , MMS_device.device_index  
                    FROM MMS_device , MMS_object 
                    WHERE MMS_device.object_id = MMS_object.id 
                      AND MMS_device.serial_no = '$serialNo'
                      AND MMS_object.id <> 0
                """.trimIndent()
            )
            val deviceConfig = if (rs.next()) {
                DeviceConfig(
                    deviceId = rs.getInt(1),
                    objectId = rs.getInt(2),
                    userId = rs.getInt(3),
                    isAutoWorkShift = rs.getInt(4) != 0,
                    deviceIndex = rs.getInt(5),
                )
            } else {
                null
            }
            rs.close()

            deviceConfig?.let { dc ->
                rs = conn.executeQuery(
                    """
                        SELECT speed_round_rule 
                        FROM MMS_sensor 
                        WHERE object_id = ${dc.objectId} 
                        AND sensor_type = ${SensorConfig.SENSOR_GEO}
                    """.trimIndent()
                )
                dc.speedRoundRule = if (rs.next()) {
                    rs.getInt(1)
                } else {
                    SensorConfigGeo.SPEED_ROUND_RULE_STANDART
                }
                rs.close()

                rs = conn.executeQuery(
                    """
                        SELECT time_offset 
                        FROM SYSTEM_users 
                        WHERE id = ${dc.userId}
                    """.trimIndent()
                )
                dc.timeZone = if (rs.next()) {
                    getTimeZone(rs.getInt(1))
                } else {
                    TimeZone.currentSystemDefault()
                }
                rs.close()
            }

            return deviceConfig
        }
    }

}

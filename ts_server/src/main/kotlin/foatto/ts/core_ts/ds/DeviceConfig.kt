package foatto.ts.core_ts.ds

import foatto.core.app.UP_TIME_OFFSET
import foatto.core.util.getZoneId
import foatto.sql.CoreAdvancedConnection
import java.time.ZoneId

class DeviceConfig {

    var deviceId = 0
    var objectId = 0
    var userId = 0

    var index = 0

    lateinit var zoneId: ZoneId

    companion object {

        fun getDeviceConfig(conn: CoreAdvancedConnection, serialNo: String, deviceType: Int): DeviceConfig? {
            var dc: DeviceConfig? = null

            var rs = conn.executeQuery(
                """
                    SELECT TS_device.id , TS_object.id , TS_object.user_id , TS_device.device_index 
                    FROM TS_object , TS_device 
                    WHERE TS_object.id = TS_device.object_id 
                    AND TS_device.serial_no = '$serialNo'
                    AND TS_device.type = $deviceType 
                """
            )

            if (rs.next()) {
                dc = DeviceConfig()
                dc.deviceId = rs.getInt(1)
                dc.objectId = rs.getInt(2)
                dc.userId = rs.getInt(3)
                dc.index = rs.getInt(4)
            }
            rs.close()

            if (dc != null) {
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

package foatto.ts.core_ts.ds

import foatto.core.app.UP_TIME_OFFSET
import foatto.core.util.getZoneId
import foatto.sql.CoreAdvancedStatement
import java.time.ZoneId

class DeviceConfig {

    var objectID = 0
    var userID = 0

    var index = 0

    lateinit var zoneId: ZoneId

    companion object {

        fun getDeviceConfig(stm: CoreAdvancedStatement, serialNo: String): DeviceConfig? {
            var dc: DeviceConfig? = null

            var rs = stm.executeQuery(
                " SELECT TS_object.id , TS_object.user_id , TS_device.device_index " +
                    " FROM TS_object , TS_device " +
                    " WHERE TS_object.id = TS_device.object_id AND TS_device.serial_no = '$serialNo'"
            )

            if (rs.next()) {
                dc = DeviceConfig()
                dc.objectID = rs.getInt(1)
                dc.userID = rs.getInt(2)
                dc.index = rs.getInt(3)
            }
            rs.close()

            if (dc != null) {
                rs = stm.executeQuery(" SELECT property_value FROM SYSTEM_user_property WHERE user_id = ${dc.userID} AND property_name = '$UP_TIME_OFFSET' ")
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

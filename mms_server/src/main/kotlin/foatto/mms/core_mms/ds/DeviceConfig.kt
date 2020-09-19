package foatto.mms.core_mms.ds

import foatto.core.app.UP_TIME_OFFSET
import foatto.core.util.getZoneId
import foatto.mms.core_mms.sensor.SensorConfig
import foatto.mms.core_mms.sensor.SensorConfigGeo
import foatto.sql.CoreAdvancedStatement
import java.time.ZoneId

class DeviceConfig {

    var objectID = 0
    var userID = 0
    var dataVersion = 0
    var isAutoWorkShift = false

    var index = 0
    var isOfflineMode = false

    var speedRoundRule = SensorConfigGeo.SPEED_ROUND_RULE_STANDART

    lateinit var zoneId: ZoneId

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {

        fun getDeviceConfig(stm: CoreAdvancedStatement, aDeviceID: Int): DeviceConfig? {
            var dc: DeviceConfig? = null

            var rs = stm.executeQuery(
                " SELECT MMS_object.id , MMS_object.user_id , MMS_object.data_version , MMS_object.is_auto_work_shift , MMS_device.device_index , MMS_device.offline_mode " +
                    " FROM MMS_object , MMS_device WHERE MMS_object.id = MMS_device.object_id AND MMS_device.device_id = $aDeviceID"
            )

            if(rs.next()) {
                dc = DeviceConfig()
                dc.objectID = rs.getInt(1)
                dc.userID = rs.getInt(2)
                dc.dataVersion = rs.getInt(3)
                dc.isAutoWorkShift = rs.getInt(4) != 0

                dc.index = rs.getInt(5)
                dc.isOfflineMode = rs.getInt(6) != 0
            }
            rs.close()

            if(dc != null) {
                rs = stm.executeQuery(" SELECT speed_round_rule FROM MMS_sensor WHERE object_id = ${dc.objectID} AND sensor_type = ${SensorConfig.SENSOR_GEO}")
                dc.speedRoundRule = if (rs.next()) rs.getInt(1) else SensorConfigGeo.SPEED_ROUND_RULE_STANDART
                rs.close()

                rs = stm.executeQuery(" SELECT property_value FROM SYSTEM_user_property WHERE user_id = ${dc.userID} AND property_name = '$UP_TIME_OFFSET' ")
                dc.zoneId = if(rs.next()) getZoneId(rs.getString(1).toInt()) else ZoneId.systemDefault()
                rs.close()
            }

            return dc
        }
    }

}

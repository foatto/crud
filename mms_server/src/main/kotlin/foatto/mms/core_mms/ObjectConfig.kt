package foatto.mms.core_mms

import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigGeo

class ObjectConfig(
    val objectId: Int,
    val userId: Int,
    val isDisabled: Boolean,
    val name: String,
    val model: String,
    val groupName: String,
    val departmentName: String,
    val info: String,
) {
    val alTitleName: MutableList<String> = mutableListOf()
    val alTitleValue: MutableList<String> = mutableListOf()

    //--- description of the sensor configuration - the first key is the sensor type, the second key is the port number
    val hmSensorConfig: MutableMap<Int, MutableMap<Int, SensorConfig>> = mutableMapOf()

    //--- separate work with a geo-sensor - it should be the only one on the object
    var scg: SensorConfigGeo? = null

    companion object {
        const val WARNING_TIME = 1 * 60 * 60
        const val CRITICAL_TIME = 24 * 60 * 60
    }
}

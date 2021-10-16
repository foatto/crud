package foatto.ts.core_ts

import foatto.ts.core_ts.sensor.config.SensorConfig

class ObjectConfig(
    val objectId: Int,
    val userId: Int,
    val name: String,
    val model: String,
) {
    val alTitleName: MutableList<String> = mutableListOf()
    val alTitleValue: MutableList<String> = mutableListOf()

    //--- description of the sensor configuration - the first key is the sensor type, the second key is the port number
    val hmSensorConfig: MutableMap<Int, MutableMap<Int, SensorConfig>> = mutableMapOf()

    companion object {
        const val WARNING_TIME = 1 * 60 * 60
        const val CRITICAL_TIME = 24 * 60 * 60
    }
}

package foatto.mms.core_mms

import foatto.core.util.AdvancedLogger
import foatto.core_server.app.server.UserConfig
import foatto.mms.core_mms.sensor.config.*
import foatto.sql.CoreAdvancedStatement

class ObjectConfig {

    var objectID = 0
    var userID = 0
    var isDisabled = false
    lateinit var name: String
    lateinit var model: String
    lateinit var groupName: String
    lateinit var departmentName: String
    lateinit var info: String

    var alTitleName = mutableListOf<String>()
    var alTitleValue = mutableListOf<String>()

    //--- description of the sensor configuration - the first key is the sensor type, the second key is the port number
    var hmSensorConfig = mutableMapOf<Int, MutableMap<Int, SensorConfig>>()

    //--- separate work with a geo-sensor - it should be the only one on the object
    var scg: SensorConfigGeo? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {

        const val WARNING_TIME = 1 * 60 * 60
        const val CRITICAL_TIME = 24 * 60 * 60

        //----------------------------------------------------------------------------------------------------------------------

        fun getObjectConfig(stm: CoreAdvancedStatement, userConfig: UserConfig, aObjectID: Int): ObjectConfig {
            val objectConfig = ObjectConfig()

            var rs = stm.executeQuery(
                " SELECT MMS_object.user_id , MMS_object.is_disabled , MMS_object.name , MMS_object.model , " +
                    " MMS_group.name , MMS_department.name , MMS_object.info " +
                    " FROM MMS_object , MMS_department , MMS_group " +
                    " WHERE MMS_object.id = $aObjectID " +
                    " AND MMS_object.group_id = MMS_group.id " +
                    " AND MMS_object.department_id = MMS_department.id "
            )
            if (rs.next()) {
                objectConfig.objectID = aObjectID
                objectConfig.userID = rs.getInt(1)
                objectConfig.isDisabled = rs.getInt(2) != 0
                val sbObjectInfo = StringBuilder(rs.getString(3))
                objectConfig.model = rs.getString(4)
                objectConfig.groupName = rs.getString(5)
                objectConfig.departmentName = rs.getString(6)
                objectConfig.info = rs.getString(7)

                //--- дополним наименование объекта его кратким логинным названием
                val shortUserName = userConfig.hmUserShortNames[objectConfig.userID]
                if (shortUserName != null && !shortUserName.isEmpty()) sbObjectInfo.append(" ( ").append(shortUserName).append(" ) ")
                objectConfig.name = sbObjectInfo.toString()
            } else {
                AdvancedLogger.error("ObjectConfig not exist for object_id = $aObjectID")
            }

            rs.close()

            objectConfig.alTitleName.add("Владелец:")
            objectConfig.alTitleValue.add(userConfig.hmUserFullNames[objectConfig.userID] ?: "(неизвестно)")
            objectConfig.alTitleName.add("Наименование:")
            objectConfig.alTitleValue.add(objectConfig.name)
            objectConfig.alTitleName.add("Модель:")
            objectConfig.alTitleValue.add(objectConfig.model)
            objectConfig.alTitleName.add("Группа:")
            objectConfig.alTitleValue.add(objectConfig.groupName)
            objectConfig.alTitleName.add("Подразделение:")
            objectConfig.alTitleValue.add(objectConfig.departmentName)

            //--- sensor configuration loading
            //--- (notes: "serial no" and "start date of operation" will be ignored)
            rs = stm.executeQuery(
                " SELECT sensor_type , port_num , id , name , sum_group_name, group_name , descr , " + // 1..7

                    " min_moving_time , min_parking_time , " +                                     // 8..9
                    " min_over_speed_time , is_absolute_run , " +                                  // 10..11
                    " speed_round_rule , run_koef , is_use_pos , is_use_speed , is_use_run , " +   // 12..16

                    " bound_value , active_value , min_on_time , min_off_time , " +         // 17..20
                    " beg_work_value , " +                                                  // 21
                    " cmd_on_id , cmd_off_id , signal_on , signal_off , " +                 // 22..25

                    " smooth_method , smooth_time , " +                                     // 26..27

                    " ignore_min_sensor , ignore_max_sensor , " +                           // 28..29

                    " liquid_name , liquid_norm , " +                                         // 30..31

                    " energo_phase , " +                                                    // 32

                    " analog_min_view , analog_max_view , " +                               // 33..34
                    " analog_min_limit , analog_max_limit , " +                             // 35..36

                    " analog_using_min_len , analog_is_using_calc , " +                     // 37..38
                    " analog_detect_inc , analog_detect_inc_min_diff , analog_detect_inc_min_len , " + // 39..41
                    " analog_inc_add_time_before , analog_inc_add_time_after , " +                     // 42..43
                    " analog_detect_dec , analog_detect_dec_min_diff , analog_detect_dec_min_len , " + // 44..46
                    " analog_dec_add_time_before , analog_dec_add_time_after " +                     // 47..48

                    " FROM MMS_sensor WHERE object_id = $aObjectID"
            )
            //--- стартовая нумерация полей
            val COMMON = 3
            val GEO = 8
            val WORK = 17
            val SMOOTH = 26
            val IGNORE = 28
            val LIQUID_NAME = 30
            val LIQUID_NORM = 31
            val PHASE = 32
            val ANALOG = 33
            val LIQUID_LEVEL = 37

            while (rs.next()) {
                val sensorType = rs.getInt(1)
                val portNum = rs.getInt(2)

                val hmSC = objectConfig.hmSensorConfig.getOrPut(sensorType, { mutableMapOf() })
                when (sensorType) {
                    SensorConfig.SENSOR_SIGNAL -> {
                        hmSC[portNum] = SensorConfigSignal(
                            aId = rs.getInt(COMMON),
                            aName = rs.getString(COMMON + 1),
                            aSumGroup = rs.getString(COMMON + 2),
                            aGroup = rs.getString(COMMON + 3),
                            aDescr = rs.getString(COMMON + 4),
                            aPortNum = portNum,
                            aSensorType = sensorType,
                            boundValue = rs.getInt(WORK),
                            activeValue = rs.getInt(WORK + 1),
                            minIgnore = rs.getDouble(IGNORE),
                            maxIgnore = rs.getDouble(IGNORE + 1)
                        )
                    }
                    SensorConfig.SENSOR_GEO -> {
                        objectConfig.scg = SensorConfigGeo(
                            aId = rs.getInt(COMMON),
                            aName = rs.getString(COMMON + 1),
                            aSumGroup = rs.getString(COMMON + 2),
                            aGroup = rs.getString(COMMON + 3),
                            aDescr = rs.getString(COMMON + 4),
                            aPortNum = portNum,
                            aSensorType = sensorType,
                            minMovingTime = rs.getInt(GEO),
                            minParkingTime = rs.getInt(GEO + 1),
                            minOverSpeedTime = rs.getInt(GEO + 2),
                            isAbsoluteRun = rs.getInt(GEO + 3) != 0,
                            speedRoundRule = rs.getInt(GEO + 4),
                            runKoef = rs.getDouble(GEO + 5),
                            isUsePos = rs.getInt(GEO + 6) != 0,
                            isUseSpeed = rs.getInt(GEO + 7) != 0,
                            isUseRun = rs.getInt(GEO + 8) != 0,
                            liquidName = rs.getString(LIQUID_NAME),
                            liquidNorm = rs.getDouble(LIQUID_NORM),
                            maxSpeedLimit = rs.getDouble(ANALOG + 4).toInt(),
                        )
                    }
                    SensorConfig.SENSOR_WORK -> {
                        hmSC[portNum] = SensorConfigWork(
                            aId = rs.getInt(COMMON),
                            aName = rs.getString(COMMON + 1),
                            aSumGroup = rs.getString(COMMON + 2),
                            aGroup = rs.getString(COMMON + 3),
                            aDescr = rs.getString(COMMON + 4),
                            aPortNum = portNum,
                            aSensorType = sensorType,
                            boundValue = rs.getInt(WORK),
                            activeValue = rs.getInt(WORK + 1),
                            minOnTime = rs.getInt(WORK + 2),
                            minOffTime = rs.getInt(WORK + 3),
                            begWorkValue = rs.getDouble(WORK + 4),
                            cmdOnID = rs.getInt(WORK + 5),
                            cmdOffID = rs.getInt(WORK + 6),
                            aSignalOn = rs.getString(WORK + 7),
                            aSignalOff = rs.getString(WORK + 8),
                            minIgnore = rs.getDouble(IGNORE),
                            maxIgnore = rs.getDouble(IGNORE + 1),
                            liquidName = rs.getString(LIQUID_NAME),
                            liquidNorm = rs.getDouble(LIQUID_NORM)
                        )
                    }
                    SensorConfig.SENSOR_MASS_FLOW, SensorConfig.SENSOR_VOLUME_FLOW -> {
                        hmSC[portNum] = SensorConfigCounter(
                            aId = rs.getInt(COMMON),
                            aName = rs.getString(COMMON + 1),
                            aSumGroup = rs.getString(COMMON + 2),
                            aGroup = rs.getString(COMMON + 3),
                            aDescr = rs.getString(COMMON + 4),
                            aPortNum = portNum,
                            aSensorType = sensorType,
                            aSmoothMethod = rs.getInt(SMOOTH),
                            aSmoothTime = rs.getInt(SMOOTH + 1) * 60,
                            aMinIgnore = rs.getDouble(IGNORE),
                            aMaxIgnore = rs.getDouble(IGNORE + 1),
                            liquidName = rs.getString(LIQUID_NAME)
                        )
                    }
                    SensorConfig.SENSOR_MASS_ACCUMULATED, SensorConfig.SENSOR_VOLUME_ACCUMULATED -> {
                        hmSC[portNum] = SensorConfigLiquidSummary(
                            aId = rs.getInt(COMMON),
                            aName = rs.getString(COMMON + 1),
                            aSumGroup = rs.getString(COMMON + 2),
                            aGroup = rs.getString(COMMON + 3),
                            aDescr = rs.getString(COMMON + 4),
                            aPortNum = portNum,
                            aSensorType = sensorType,
                            aSmoothMethod = rs.getInt(SMOOTH),
                            aSmoothTime = rs.getInt(SMOOTH + 1) * 60,
                            aMinIgnore = rs.getDouble(IGNORE),
                            aMaxIgnore = rs.getDouble(IGNORE + 1),
                            liquidName = rs.getString(LIQUID_NAME)
                        )
                    }
                    SensorConfig.SENSOR_ENERGO_COUNT_AD, SensorConfig.SENSOR_ENERGO_COUNT_AR,
                    SensorConfig.SENSOR_ENERGO_COUNT_RD, SensorConfig.SENSOR_ENERGO_COUNT_RR -> {
                        hmSC[portNum] = SensorConfigEnergoSummary(
                            aId = rs.getInt(COMMON),
                            aName = rs.getString(COMMON + 1),
                            aSumGroup = rs.getString(COMMON + 2),
                            aGroup = rs.getString(COMMON + 3),
                            aDescr = rs.getString(COMMON + 4),
                            aPortNum = portNum,
                            aSensorType = sensorType,
                            aSmoothMethod = rs.getInt(SMOOTH),
                            aSmoothTime = rs.getInt(SMOOTH + 1) * 60,
                            aMinIgnore = rs.getDouble(IGNORE),
                            aMaxIgnore = rs.getDouble(IGNORE + 1),
                            phase = rs.getInt(PHASE),
                        )
                    }
                    SensorConfig.SENSOR_LIQUID_FLOW_CALC, SensorConfig.SENSOR_WEIGHT,
                    SensorConfig.SENSOR_TURN, SensorConfig.SENSOR_PRESSURE,
                    SensorConfig.SENSOR_TEMPERATURE, SensorConfig.SENSOR_VOLTAGE,
                    SensorConfig.SENSOR_POWER, SensorConfig.SENSOR_DENSITY -> {
                        hmSC[portNum] = SensorConfigAnalogue(
                            aId = rs.getInt(COMMON),
                            aName = rs.getString(COMMON + 1),
                            aSumGroup = rs.getString(COMMON + 2),
                            aGroup = rs.getString(COMMON + 3),
                            aDescr = rs.getString(COMMON + 4),
                            aPortNum = portNum,
                            aSensorType = sensorType,
                            aSmoothMethod = rs.getInt(SMOOTH),
                            aSmoothTime = rs.getInt(SMOOTH + 1) * 60,
                            aMinIgnore = rs.getDouble(IGNORE),
                            aMaxIgnore = rs.getDouble(IGNORE + 1),
                            minView = rs.getDouble(ANALOG),
                            maxView = rs.getDouble(ANALOG + 1),
                            minLimit = rs.getDouble(ANALOG + 2),
                            maxLimit = rs.getDouble(ANALOG + 3),
                        )
                    }
                    SensorConfig.SENSOR_ENERGO_VOLTAGE, SensorConfig.SENSOR_ENERGO_CURRENT,
                    SensorConfig.SENSOR_ENERGO_POWER_KOEF, SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                    SensorConfig.SENSOR_ENERGO_POWER_REACTIVE, SensorConfig.SENSOR_ENERGO_POWER_FULL -> {
                        hmSC[portNum] = SensorConfigEnergoAnalogue(
                            aId = rs.getInt(COMMON),
                            aName = rs.getString(COMMON + 1),
                            aSumGroup = rs.getString(COMMON + 2),
                            aGroup = rs.getString(COMMON + 3),
                            aDescr = rs.getString(COMMON + 4),
                            aPortNum = portNum,
                            aSensorType = sensorType,
                            aSmoothMethod = rs.getInt(SMOOTH),
                            aSmoothTime = rs.getInt(SMOOTH + 1) * 60,
                            aMinIgnore = rs.getDouble(IGNORE),
                            aMaxIgnore = rs.getDouble(IGNORE + 1),
                            phase = rs.getInt(PHASE),
                            aMinView = rs.getDouble(ANALOG),
                            aMaxView = rs.getDouble(ANALOG + 1),
                            aMinLimit = rs.getDouble(ANALOG + 2),
                            aMaxLimit = rs.getDouble(ANALOG + 3),
                        )
                    }
                    SensorConfig.SENSOR_LIQUID_LEVEL -> {
                        hmSC[portNum] = SensorConfigLiquidLevel(
                            aId = rs.getInt(COMMON),
                            aName = rs.getString(COMMON + 1),
                            aSumGroup = rs.getString(COMMON + 2),
                            aGroup = rs.getString(COMMON + 3),
                            aDescr = rs.getString(COMMON + 4),
                            aPortNum = portNum,
                            aSensorType = sensorType,
                            aSmoothMethod = rs.getInt(SMOOTH),
                            aSmoothTime = rs.getInt(SMOOTH + 1) * 60,
                            aMinIgnore = rs.getDouble(IGNORE),
                            aMaxIgnore = rs.getDouble(IGNORE + 1),
                            liquidName = rs.getString(LIQUID_NAME),
                            aMinView = rs.getDouble(ANALOG),
                            aMaxView = rs.getDouble(ANALOG + 1),
                            aMinLimit = rs.getDouble(ANALOG + 2),
                            aMaxLimit = rs.getDouble(ANALOG + 3),
                            usingMinLen = rs.getInt(LIQUID_LEVEL),
                            isUsingCalc = rs.getInt(LIQUID_LEVEL + 1) != 0,
                            detectIncKoef = rs.getDouble(LIQUID_LEVEL + 2),
                            detectIncMinDiff = rs.getDouble(LIQUID_LEVEL + 3),
                            detectIncMinLen = rs.getInt(LIQUID_LEVEL + 4),
                            incAddTimeBefore = rs.getInt(LIQUID_LEVEL + 5),
                            incAddTimeAfter = rs.getInt(LIQUID_LEVEL + 6),
                            detectDecKoef = rs.getDouble(LIQUID_LEVEL + 7),
                            detectDecMinDiff = rs.getDouble(LIQUID_LEVEL + 8),
                            detectDecMinLen = rs.getInt(LIQUID_LEVEL + 9),
                            decAddTimeBefore = rs.getInt(LIQUID_LEVEL + 10),
                            decAddTimeAfter = rs.getInt(LIQUID_LEVEL + 11),
                        )
                    }
                    else -> {
                        AdvancedLogger.error("Unknown sensorType = $sensorType for sensorId = ${rs.getInt(COMMON)}")
                    }
                }
            }
            rs.close()

            //--- download of calibration / calibration of sensors
            objectConfig.hmSensorConfig.entries.forEach { entry ->
                entry.value.values.forEach { sc ->
                    if (sc is SensorConfigBase) {
                        rs = stm.executeQuery(" SELECT value_sensor , value_data FROM MMS_sensor_calibration WHERE sensor_id = ${sc.id} ORDER BY value_sensor ")
                        while (rs.next()) {
                            sc.alValueSensor.add(rs.getDouble(1))
                            sc.alValueData.add(rs.getDouble(2))
                        }
                        rs.close()
                    }
                }
            }

            return objectConfig
        }
    }
}

package foatto.mms.core_mms

import foatto.core.util.AdvancedLogger
import foatto.core_server.app.server.UserConfig
import foatto.mms.core_mms.sensor.*
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
    var dataVersion = 0

    var alTitleName = mutableListOf<String>()
    var alTitleValue = mutableListOf<String>()

    //--- описание конфигурации датчиков - первый ключ - тип датчика, второй ключ - номер порта
    //--- ( поскольку расход жидкости может измеряться по изменению уровня, то требуется быстрый поиск по номеру порта )
    var hmSensorConfig = mutableMapOf<Int, MutableMap<Int, SensorConfig>>()

    //--- отдельная работа с гео-датчиком - он д.б. один на объекте
    var scg: SensorConfigGeo? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {

        //--- Пока зададим в виде констант:
        //--- нет данных более часа
        const val WARNING_TIME = 1 * 60 * 60

        //--- нет данных более суток
        const val CRITICAL_TIME = 24 * 60 * 60

        //----------------------------------------------------------------------------------------------------------------------

        fun getObjectConfig(stm: CoreAdvancedStatement, userConfig: UserConfig, aObjectID: Int): ObjectConfig {
            val objectConfig = ObjectConfig()

            var rs = stm.executeQuery(
                " SELECT MMS_object.user_id , MMS_object.is_disabled , MMS_object.name , MMS_object.model , " +
                    " MMS_group.name , MMS_department.name , MMS_object.info , MMS_object.data_version " +
                    " FROM MMS_object , MMS_department , MMS_group " +
                    " WHERE MMS_object.id = $aObjectID " +
                    " AND MMS_object.group_id = MMS_group.id " +
                    " AND MMS_object.department_id = MMS_department.id "
            )
            if(rs.next()) {
                objectConfig.objectID = aObjectID
                objectConfig.userID = rs.getInt(1)
                objectConfig.isDisabled = rs.getInt(2) != 0
                val sbObjectInfo = StringBuilder(rs.getString(3))
                objectConfig.model = rs.getString(4)
                objectConfig.groupName = rs.getString(5)
                objectConfig.departmentName = rs.getString(6)
                objectConfig.info = rs.getString(7)
                objectConfig.dataVersion = rs.getInt(8)

                //--- дополним наименование объекта его кратким логинным названием
                val shortUserName = userConfig.hmUserShortNames[objectConfig.userID]
                if(shortUserName != null && !shortUserName.isEmpty()) sbObjectInfo.append(" ( ").append(shortUserName).append(" ) ")
                objectConfig.name = sbObjectInfo.toString()
            } else AdvancedLogger.error("ObjectConfig not exist for object_id = $aObjectID")

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

            //--- загрузка конфигурации датчиков
            rs = stm.executeQuery(
                " SELECT sensor_type , port_num , id , name , sum_group_name, group_name , descr , " + // 1..7

                    " min_moving_time , min_parking_time , " +                                     // 8..9
                    " min_over_speed_time , is_absolute_run , " +                                  // 10..11
                    " speed_round_rule , run_koef , is_use_pos , is_use_speed , is_use_run , " +   // 12..16

                    " bound_value , active_value , min_on_time , min_off_time , " +        // 17..20
                    " calc_in_moving , calc_in_parking , beg_work_value , " +              // 21..23
                    " cmd_on_id , cmd_off_id , signal_on , signal_off , " +                // 24..27

                    " count_value_sensor , count_value_data , " +                          // 28..29

                    " analog_dim , analog_min_view , analog_max_view , " +                 // 30..32
                    " analog_min_limit , analog_max_limit , " +                            // 33..34

                    " analog_using_min_len , analog_is_using_calc , " +                    // 35..36
                    " analog_detect_inc , analog_detect_inc_min_diff , analog_detect_inc_min_len , " + // 37..39
                    " analog_inc_add_time_before , analog_inc_add_time_after , " +                     // 40..41
                    " analog_detect_dec , analog_detect_dec_min_diff , analog_detect_dec_min_len , " + // 42..44
                    " analog_dec_add_time_before , analog_dec_add_time_after , " +                     // 45..46
                    " smooth_method , smooth_time , " +                                                // 47..48

                    " ignore_min_sensor , ignore_max_sensor , " +                          // 49..50

                    " energo_phase , " +                                                  // 51

                    " liquid_name , liquid_norm " +                                        // 52..53
                    " FROM MMS_sensor WHERE object_id = $aObjectID"
            )
            //--- стартовая нумерация полей
            val COMMON = 3
            val GEO = 8
            val WORK = 17
            val LU = 28
            val ANALOG = 30
            val IGNORE = 49
            val ENERGO = 51
            val LI_NA = 52
            val LI_NO = 53
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
                            minIgnore = rs.getInt(IGNORE),
                            maxIgnore = rs.getInt(IGNORE + 1)
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
                            isAbsoluteRun = rs.getInt(GEO + 3) == 1,
                            speedRoundRule = rs.getInt(GEO + 4),
                            runKoef = rs.getDouble(GEO + 5),
                            isUsePos = rs.getInt(GEO + 6) == 1,
                            isUseSpeed = rs.getInt(GEO + 7) == 1,
                            isUseRun = rs.getInt(GEO + 8) == 1,
                            maxSpeedLimit = rs.getDouble(ANALOG + 4).toInt(),
                            liquidName = rs.getString(LI_NA),
                            liquidNorm = rs.getDouble(LI_NO)
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
                            calcInMoving = rs.getInt(WORK + 4) == 1,
                            calcInParking = rs.getInt(WORK + 5) == 1,
                            begWorkValue = rs.getDouble(WORK + 6),
                            cmdOnID = rs.getInt(WORK + 7),
                            cmdOffID = rs.getInt(WORK + 8),
                            aSignalOn = rs.getString(WORK + 9),
                            aSignalOff = rs.getString(WORK + 10),
                            minIgnore = rs.getInt(IGNORE),
                            maxIgnore = rs.getInt(IGNORE + 1),
                            liquidName = rs.getString(LI_NA),
                            liquidNorm = rs.getDouble(LI_NO)
                        )
                    }
                    /*SensorConfig.SENSOR_LIQUID_USING, */ SensorConfig.SENSOR_MASS_FLOW, SensorConfig.SENSOR_VOLUME_FLOW -> {
                    hmSC[portNum] = SensorConfigUsing(
                        aId = rs.getInt(COMMON),
                        aName = rs.getString(COMMON + 1),
                        aSumGroup = rs.getString(COMMON + 2),
                        aGroup = rs.getString(COMMON + 3),
                        aDescr = rs.getString(COMMON + 4),
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        sensorValue = rs.getInt(LU),
                        dataValue = rs.getDouble(LU + 1),
                        minIgnore = rs.getInt(IGNORE),
                        maxIgnore = rs.getInt(IGNORE + 1),
                        aLiquidName = rs.getString(LI_NA)
                    )
                }
                    SensorConfig.SENSOR_MASS_ACCUMULATED, SensorConfig.SENSOR_VOLUME_ACCUMULATED,
                    SensorConfig.SENSOR_ENERGO_COUNT_AD, SensorConfig.SENSOR_ENERGO_COUNT_AR,
                    SensorConfig.SENSOR_ENERGO_COUNT_RD, SensorConfig.SENSOR_ENERGO_COUNT_RR -> {
                        hmSC[portNum] = SensorConfig(
                            id = rs.getInt(COMMON),
                            name = rs.getString(COMMON + 1),
                            sumGroup = rs.getString(COMMON + 2),
                            group = rs.getString(COMMON + 3),
                            descr = rs.getString(COMMON + 4),
                            portNum = portNum,
                            sensorType = sensorType
                        )
                    }
                    SensorConfig.SENSOR_ENERGO_VOLTAGE, SensorConfig.SENSOR_ENERGO_CURRENT,
                    SensorConfig.SENSOR_ENERGO_POWER_KOEF, SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                    SensorConfig.SENSOR_ENERGO_POWER_REACTIVE, SensorConfig.SENSOR_ENERGO_POWER_FULL -> {
                        hmSC[portNum] = SensorConfigElectro(
                            aId = rs.getInt(COMMON),
                            aName = rs.getString(COMMON + 1),
                            aSumGroup = rs.getString(COMMON + 2),
                            aGroup = rs.getString(COMMON + 3),
                            aDescr = rs.getString(COMMON + 4),
                            aPortNum = portNum,
                            aSensorType = sensorType,
                            dim = rs.getString(ANALOG),
                            minView = rs.getDouble(ANALOG + 1),
                            maxView = rs.getDouble(ANALOG + 2),
                            minLimit = rs.getDouble(ANALOG + 3),
                            maxLimit = rs.getDouble(ANALOG + 4),
                            smoothMethod = rs.getInt(ANALOG + 17),
                            smoothTime = rs.getInt(ANALOG + 18) * 60,
                            minIgnore = rs.getInt(IGNORE),
                            maxIgnore = rs.getInt(IGNORE + 1),
                            phase = rs.getInt(ENERGO)
                        )
                    }
                    else -> {
                        hmSC[portNum] = SensorConfigAnalogue(
                            aId = rs.getInt(COMMON),
                            aName = rs.getString(COMMON + 1),
                            aSumGroup = rs.getString(COMMON + 2),
                            aGroup = rs.getString(COMMON + 3),
                            aDescr = rs.getString(COMMON + 4),
                            aPortNum = portNum,
                            aSensorType = sensorType,
                            dim = rs.getString(ANALOG),
                            minView = rs.getDouble(ANALOG + 1),
                            maxView = rs.getDouble(ANALOG + 2),
                            minLimit = rs.getDouble(ANALOG + 3),
                            maxLimit = rs.getDouble(ANALOG + 4),
                            usingMinLen = rs.getInt(ANALOG + 5),
                            isUsingCalc = rs.getInt(ANALOG + 6) == 1,
                            detectIncKoef = rs.getDouble(ANALOG + 7),
                            detectIncMinDiff = rs.getDouble(ANALOG + 8),
                            detectIncMinLen = rs.getInt(ANALOG + 9),
                            incAddTimeBefore = rs.getInt(ANALOG + 10),
                            incAddTimeAfter = rs.getInt(ANALOG + 11),
                            detectDecKoef = rs.getDouble(ANALOG + 12),
                            detectDecMinDiff = rs.getDouble(ANALOG + 13),
                            detectDecMinLen = rs.getInt(ANALOG + 14),
                            decAddTimeBefore = rs.getInt(ANALOG + 15),
                            decAddTimeAfter = rs.getInt(ANALOG + 16),
                            smoothMethod = rs.getInt(ANALOG + 17),
                            smoothTime = rs.getInt(ANALOG + 18) * 60,
                            minIgnore = rs.getInt(IGNORE),
                            maxIgnore = rs.getInt(IGNORE + 1),
                            liquidName = rs.getString(LI_NA)
                        )
                    }
                }
            }
            rs.close()

            //--- загрузка калибровки/тарировки аналоговых датчиков
            objectConfig.hmSensorConfig.entries.filterNot { entry ->
                //--- некалибруемые датчики пропускаем
                SensorConfig.hsSensorNonCalibration.contains(entry.key)
            }
            .forEach { entry ->
                entry.value.values.forEach { sc ->
                    val scCalibrable = sc as SensorConfigSemiAnalogue
                    rs = stm.executeQuery(" SELECT value_sensor , value_data FROM MMS_sensor_calibration WHERE sensor_id = ${scCalibrable.id} ORDER BY value_sensor ")
                    while (rs.next()) {
                        scCalibrable.alValueSensor.add(rs.getDouble(1))
                        scCalibrable.alValueData.add(rs.getDouble(2))
                    }
                    rs.close()
                }
            }

            return objectConfig
        }
    }
}

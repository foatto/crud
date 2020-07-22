package foatto.mms.core_mms

import foatto.core.util.AdvancedLogger
import foatto.core_server.app.server.UserConfig
import foatto.mms.core_mms.sensor.SensorConfig
import foatto.mms.core_mms.sensor.SensorConfigA
import foatto.mms.core_mms.sensor.SensorConfigG
import foatto.mms.core_mms.sensor.SensorConfigS
import foatto.mms.core_mms.sensor.SensorConfigU
import foatto.mms.core_mms.sensor.SensorConfigW
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
    var scg: SensorConfigG? = null

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
                StringBuilder(" SELECT sensor_type , port_num , id , name , sum_group_name, group_name , descr , ") // 1..7

                    .append(" min_moving_time , min_parking_time , ")                                     // 8..9
                    .append(" min_over_speed_time , is_absolute_run , ")                                  // 10..11
                    .append(" speed_round_rule , run_koef , is_use_pos , is_use_speed , is_use_run , ")   // 12..16

                    .append(" bound_value , active_value , min_on_time , min_off_time , ")        // 17..20
                    .append(" calc_in_moving , calc_in_parking , beg_work_value , ")              // 21..23
                    .append(" cmd_on_id , cmd_off_id , signal_on , signal_off , ")                // 24..27

                    .append(" count_value_sensor , count_value_data , ")                          // 28..29

                    .append(" analog_dim , analog_min_view , analog_max_view , ")                 // 30..32
                    .append(" analog_min_limit , analog_max_limit , ")                            // 33..34

                    .append(" analog_using_min_len , analog_is_using_calc , ")                    // 35..36
                    .append(" analog_detect_inc , analog_detect_inc_min_diff , analog_detect_inc_min_len , ") // 37..39
                    .append(" analog_inc_add_time_before , analog_inc_add_time_after , ")                     // 40..41
                    .append(" analog_detect_dec , analog_detect_dec_min_diff , analog_detect_dec_min_len , ") // 42..44
                    .append(" analog_dec_add_time_before , analog_dec_add_time_after , ")                     // 45..46
                    .append(" smooth_method , smooth_time , ")                                                // 47..48

                    .append(" ignore_min_sensor , ignore_max_sensor , ")                          // 49..50

                    .append(" liquid_name , liquid_norm ")                                        // 51..52
                    .append(" FROM MMS_sensor WHERE object_id = ").append(aObjectID).toString()
            )
            //--- стартовая нумерация полей
            val COMMON = 3
            val GEO = 8
            val WORK = 17
            val LU = 28
            val ANALOG = 30
            val IGNORE = 49
            val LI_NA = 51
            val LI_NO = 52
            while(rs.next()) {
                val sensorType = rs.getInt(1)
                val portNum = rs.getInt(2)

                val hmSC = objectConfig.hmSensorConfig.getOrPut(sensorType, { mutableMapOf() })
                when(sensorType) {
                    SensorConfig.SENSOR_SIGNAL -> {
                        hmSC[portNum] = SensorConfigS(
                            rs.getInt(COMMON), rs.getString(COMMON + 1), rs.getString(COMMON + 2), rs.getString(COMMON + 3), rs.getString(COMMON + 4),
                            portNum, sensorType,
                            rs.getInt(WORK), rs.getInt(WORK + 1),
                            rs.getInt(IGNORE), rs.getInt(IGNORE + 1)
                        )
                    }
                    SensorConfig.SENSOR_GEO -> {
                        objectConfig.scg = SensorConfigG(
                            rs.getInt(COMMON), rs.getString(COMMON + 1), rs.getString(COMMON + 2), rs.getString(COMMON + 3), rs.getString(COMMON + 4),
                            portNum, sensorType,
                            rs.getInt(GEO), rs.getInt(GEO + 1), rs.getInt(GEO + 2), rs.getInt(GEO + 3) == 1, rs.getInt(GEO + 4),
                            rs.getDouble(GEO + 5), rs.getInt(GEO + 6) == 1, rs.getInt(GEO + 7) == 1, rs.getInt(GEO + 8) == 1,
                            rs.getDouble(ANALOG + 4).toInt(),
                            rs.getString(LI_NA), rs.getDouble(LI_NO)
                        )
                    }
                    SensorConfig.SENSOR_WORK -> {
                        hmSC[portNum] = SensorConfigW(
                            rs.getInt(COMMON), rs.getString(COMMON + 1), rs.getString(COMMON + 2), rs.getString(COMMON + 3), rs.getString(COMMON + 4),
                            portNum, sensorType,
                            rs.getInt(WORK), rs.getInt(WORK + 1), rs.getInt(WORK + 2), rs.getInt(WORK + 3), rs.getInt(WORK + 4) == 1,
                            rs.getInt(WORK + 5) == 1, rs.getDouble(WORK + 6), rs.getInt(WORK + 7), rs.getInt(WORK + 8), rs.getString(WORK + 9), rs.getString(WORK + 10),
                            rs.getInt(IGNORE), rs.getInt(IGNORE + 1),
                            rs.getString(LI_NA), rs.getDouble(LI_NO)
                        )
                    }
                    /*SensorConfig.SENSOR_LIQUID_USING, */ SensorConfig.SENSOR_MASS_FLOW, SensorConfig.SENSOR_VOLUME_FLOW -> {
                        hmSC[portNum] = SensorConfigU(
                            rs.getInt(COMMON), rs.getString(COMMON + 1), rs.getString(COMMON + 2), rs.getString(COMMON + 3), rs.getString(COMMON + 4),
                            portNum, sensorType,
                            rs.getInt(LU), rs.getDouble(LU + 1),
                            rs.getInt(IGNORE), rs.getInt(IGNORE + 1),
                            rs.getString(LI_NA)
                        )
                    }
                    SensorConfig.SENSOR_MASS_ACCUMULATED, SensorConfig.SENSOR_VOLUME_ACCUMULATED,
                    SensorConfig.SENSOR_ENERGO_COUNT_AD, SensorConfig.SENSOR_ENERGO_COUNT_AR,
                    SensorConfig.SENSOR_ENERGO_COUNT_RD, SensorConfig.SENSOR_ENERGO_COUNT_RR -> {
                        hmSC[portNum] = SensorConfig(
                            rs.getInt(COMMON), rs.getString(COMMON + 1), rs.getString(COMMON + 2), rs.getString(COMMON + 3), rs.getString(COMMON + 4),
                            portNum, sensorType
                        )
                    }
                    else -> {
                        hmSC[portNum] = SensorConfigA(
                            rs.getInt(COMMON), rs.getString(COMMON + 1), rs.getString(COMMON + 2), rs.getString(COMMON + 3), rs.getString(COMMON + 4),
                            portNum, sensorType,
                            rs.getString(ANALOG), rs.getDouble(ANALOG + 1), rs.getDouble(ANALOG + 2), rs.getDouble(ANALOG + 3), rs.getDouble(ANALOG + 4),
                            rs.getInt(ANALOG + 5), rs.getInt(ANALOG + 6) == 1, rs.getDouble(ANALOG + 7), rs.getDouble(ANALOG + 8), rs.getInt(ANALOG + 9),
                            rs.getInt(ANALOG + 10), rs.getInt(ANALOG + 11), rs.getDouble(ANALOG + 12), rs.getDouble(ANALOG + 13), rs.getInt(ANALOG + 14),
                            rs.getInt(ANALOG + 15), rs.getInt(ANALOG + 16), rs.getInt(ANALOG + 17), rs.getInt(ANALOG + 18) * 60,
                            rs.getInt(IGNORE), rs.getInt(IGNORE + 1),
                            rs.getString(LI_NA)
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
                    val scA = sc as SensorConfigA
                    rs = stm.executeQuery(" SELECT value_sensor , value_data FROM MMS_sensor_calibration WHERE sensor_id = ${scA.id} ORDER BY value_sensor ")
                    while(rs.next()) {
                        scA.alValueSensor.add(rs.getDouble(1))
                        scA.alValueData.add(rs.getDouble(2))
                    }
                    rs.close()
                }
            }

            return objectConfig
        }
    }
}

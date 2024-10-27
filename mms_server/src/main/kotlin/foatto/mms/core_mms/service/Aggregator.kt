////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//        private val alPreparedSensorTypes = listOf(
//            //--- counter sensors
////            SensorConfig.SENSOR_MASS_ACCUMULATED,
////            SensorConfig.SENSOR_VOLUME_ACCUMULATED,
////            SensorConfig.SENSOR_LIQUID_USING,
////            SensorConfig.SENSOR_ENERGO_COUNT_AD,
////            SensorConfig.SENSOR_ENERGO_COUNT_AR,
////            SensorConfig.SENSOR_ENERGO_COUNT_RD,
////            SensorConfig.SENSOR_ENERGO_COUNT_RR,
//            //--- analogue sensors
//            SensorConfig.SENSOR_LIQUID_LEVEL,
////            SensorConfig.SENSOR_WEIGHT,
////            SensorConfig.SENSOR_TURN,
////            SensorConfig.SENSOR_PRESSURE,
////            SensorConfig.SENSOR_TEMPERATURE,
////            SensorConfig.SENSOR_VOLTAGE,
////            SensorConfig.SENSOR_POWER,
////            SensorConfig.SENSOR_DENSITY,
////            SensorConfig.SENSOR_MASS_FLOW,
////            SensorConfig.SENSOR_VOLUME_FLOW,
////            SensorConfig.SENSOR_ENERGO_VOLTAGE,
////            SensorConfig.SENSOR_ENERGO_CURRENT,
////            SensorConfig.SENSOR_ENERGO_POWER_KOEF,
////            SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
////            SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
////            SensorConfig.SENSOR_ENERGO_POWER_FULL,
//            //--- equipment work sensor
////            SensorConfig.SENSOR_WORK,
//        )
//
//        /*
//        Оставшиеся типы датчиков:
//        SENSOR_SIGNAL = -2   // Есть/нет
//        SENSOR_GEO = -1           // координаты, км/ч, м или км
//        SENSOR_WORK = 1           // мото * час
//        SENSOR_LIQUID_FLOW_CALC,
//        SENSOR_LIQUID_USING_COUNTER_STATE = 17
//         */
//        private const val BATCH_SIZE = "batch_size"
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    private val zoneId = ZoneId.systemDefault()

////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//                when (sensorInfo.sensorType) {
//                    SensorConfig.SENSOR_MASS_ACCUMULATED,
//                    SensorConfig.SENSOR_VOLUME_ACCUMULATED,
//                    SensorConfig.SENSOR_LIQUID_USING,
//                    SensorConfig.SENSOR_ENERGO_COUNT_AD,
//                    SensorConfig.SENSOR_ENERGO_COUNT_AR,
//                    SensorConfig.SENSOR_ENERGO_COUNT_RD,
//                    SensorConfig.SENSOR_ENERGO_COUNT_RR,
//                    -> {
//                        calcCounter(
//                            sensorInfo = sensorInfo,
//                            alValueSensor = alValueSensor,
//                            alValueData = alValueData,
//                        )
//                    }
//
//                    SensorConfig.SENSOR_WORK -> {
//                        calcWork(sensorInfo = sensorInfo)
//                    }
//
//            }
//
////--- COUNTER SENSORS PART
//
//    private fun calcCounter(
//        sensorInfo: SensorInfo,
//        alValueSensor: List<Double>,
//        alValueData: List<Double>,
//    ) {
//        val (alRawTime, alRawData) = startUpSensorDatas(sensorInfo)
//
//        var prevSumValue = getLastAggregatedCounterValue(sensorInfo.sensorId)
//
//        var curTime: Int? = null
//        for (pos in alRawTime.indices) {
//            curTime = alRawTime[pos]
//
//            val sensorData = AbstractObjectStateCalc.getSensorData(sensorInfo.portNum, alRawData[pos])?.toDouble() ?: continue
//
//            //--- ignore outbound values
//            if (ObjectCalc.isIgnoreSensorData(sensorInfo.minIgnore, sensorInfo.maxIgnore, sensorData)) {
//                continue
//            }
//
//            val sensorValue = AbstractObjectStateCalc.getSensorValue(alValueSensor, alValueData, sensorData)
//
//            val storeValue = if (sensorInfo.isAbsoluteCount) {
//                if (prevSumValue == null || sensorValue < prevSumValue) {
//                    Triple(curTime, 0.0, sensorValue)
//                } else {
//                    Triple(curTime, sensorValue - prevSumValue, sensorValue)
//                }
//            } else {
//                Triple(curTime, sensorValue, 0.0)
//            }
//            prevSumValue = sensorValue
//
//            storeCounterData(sensorInfo.sensorId, storeValue)
//        }
//
//        curTime?.let {
//            refreshCalcTime(sensorInfo.sensorId, curTime)
//            AdvancedLogger.debug("Calc finished: new calc_time = ${DateTime_YMDHMS(zoneId, curTime)}")
//        }
//    }
//
//    private fun getLastAggregatedCounterValue(sensorId: Int): Double? {
//        val agRs = alConn[0].executeQuery(
//            """
//                SELECT value_2
//                FROM MMS_agg_${sensorId}
//                ORDER BY ontime DESC
//                LIMIT 1
//            """
//        )
//        val result = if (agRs.next()) {
//            agRs.getDouble(1)
//        } else {
//            null
//        }
//        agRs.close()
//
//        return result
//    }
//
//    private fun storeCounterData(sensorId: Int, storeValue: Triple<Int, Double, Double>) {
//        alConn[0].executeUpdate(
//            """
//                INSERT INTO MMS_agg_${sensorId} (
//                    ontime , value_1 , value_2
//                ) VALUES (
//                    ${storeValue.first} , ${storeValue.second} , ${storeValue.third}
//                )
//            """
//        )
//    }
//
////--- ANALOGUE SENSORS PART
//
//    private fun calcAnalogue(
//        sensorInfo: SensorInfo,
//        alValueSensor: List<Double>,
//        alValueData: List<Double>,
//    ) {
//
//        var curTime: Int? = null
//        for (pos in alRawTime.indices) {
//            curTime = alRawTime[pos]
//
//
//
//        }
//
//    }
//    /*
//        if (gldLast == null || rawTime - gldLast.x > xScale || abs(rawData - gldLast.y) > yScale || curColorIndex != gldLast.colorIndex) {
//            val gd = scg?.let {
//                AbstractObjectStateCalc.getGeoData(scg, alRawData[pos])
//            }
//            alGLD.add(GraphicLineData(rawTime, avgValue, curColorIndex, gd?.let { XyProjection.wgs_pix(gd.wgs) }))
//        }
//     */
//
//    private fun getFirstNonAveragedTime(
//        sensorInfo: SensorInfo,
//        curTime: Int,
//    ): Int? {
//        val rs = alConn[0].executeQuery(
//            """
//                SELECT ontime
//                FROM MMS_agg_${sensorInfo.sensorId}
//                WHERE ontime <= ${curTime - sensorInfo.smoothTime}
//                AND value_2 IS NULL
//                ORDER BY ontime ASC
//                LIMIT 1
//            """
//        )
//        val nonAvgOntime = if (rs.next()) {
//            rs.getInt(1)
//        } else {
//            null
//        }
//        rs.close()
//
//        return nonAvgOntime
//    }
//
//    private fun getRawValuesForAveraging(
//        sensorInfo: SensorInfo,
//        avgOntime: Int,
//    ): MutableList<Double> {
//        val alRawValues = mutableListOf<Double>()
//
//        val rs = alConn[0].executeQuery(
//            """
//                SELECT value_1
//                FROM MMS_agg_${sensorInfo.sensorId}
//                WHERE ontime >= ${avgOntime - sensorInfo.smoothTime}
//                AND ontime <= ${avgOntime + sensorInfo.smoothTime}
//            """
//        )
//        while (rs.next()) {
//            alRawValues += rs.getDouble(1)
//        }
//        rs.close()
//
//        return alRawValues
//    }
//
//    private fun getPrevData(
//        sensorInfo: SensorInfo,
//        avgOntime: Int,
//        avgValue: Double,
//    ): Pair<Int, Double> {
//        val rs = alConn[0].executeQuery(
//            """
//                SELECT ontime , value_2
//                FROM MMS_agg_${sensorInfo.sensorId}
//                WHERE ontime < $avgOntime
//                ORDER BY ontime DESC
//                LIMIT 1
//            """
//        )
//        val prevData = if (rs.next()) {
//            rs.getInt(1) to rs.getDouble(2)
//        } else {
//            avgOntime to avgValue
//        }
//        rs.close()
//
//        return prevData
//    }
//
//    private fun getValueTypeForLiquidLevelSensors(
//        sensorInfo: SensorInfo,
//        avgOntime: Int,
//        avgValue: Double,
//        prevTime: Int,
//        prevValue: Double,
//    ): AnalogueValueType =
//        if (avgOntime - prevTime > ObjectCalc.MAX_WORK_TIME_INTERVAL) {
//            AnalogueValueType.NONE
//        } else if (avgOntime == prevTime) {
//            AnalogueValueType.NORMAL
//        } else {
//            val liquidKoef = (avgValue - prevValue) * 3600 / (avgOntime - prevTime)
//            if (liquidKoef > 0 && liquidKoef > sensorInfo.detectIncKoef) {
//                AnalogueValueType.ABOVE
//            } else if (liquidKoef < 0 && -liquidKoef > sensorInfo.detectDecKoef) {
//                AnalogueValueType.BELOW
//            } else {
//                AnalogueValueType.NORMAL
//            }
//        }
//
//    private fun getValueTypeForAnalogueSensors(
//        sensorInfo: SensorInfo,
//        avgOntime: Int,
//        avgValue: Double,
//        prevTime: Int,
//    ) =
//        if (avgOntime - prevTime > ObjectCalc.MAX_WORK_TIME_INTERVAL) {
//            AnalogueValueType.NONE
//        } else if (sensorInfo.analogMinLimit != sensorInfo.analogMaxLimit) {
//            if (avgValue < sensorInfo.analogMinLimit) {
//                AnalogueValueType.BELOW
//            } else if (avgValue > sensorInfo.analogMaxLimit) {
//                AnalogueValueType.ABOVE
//            } else {
//                AnalogueValueType.NORMAL
//            }
//        } else {
//            AnalogueValueType.NORMAL
//        }
//
//    private fun storeNonAveragedAnalogueData(
//        sensorInfo: SensorInfo,
//        ontime: Int,
//        value: Double,
//        valueType: AnalogueValueType,
//    ) {
//        alConn[0].executeUpdate(
//            """
//                INSERT INTO MMS_agg_${sensorInfo.sensorId} (
//                    ontime , value_1 , value_2 , type_1
//                ) VALUES (
//                    $ontime , $value , $value , ${valueType.value}
//                )
//            """
//        )
//    }
//
//    private fun storeRawAnalogueData(
//        sensorInfo: SensorInfo,
//        ontime: Int,
//        value: Double,
//    ) {
//        alConn[0].executeUpdate(
//            """
//                INSERT INTO MMS_agg_${sensorInfo.sensorId} (
//                    ontime , value_1
//                ) VALUES (
//                    $ontime , $value
//                )
//            """
//        )
//    }
//
//    private fun storeAverageValueAndType(
//        sensorInfo: SensorInfo,
//        avgOntime: Int,
//        avgValue: Double,
//        valueType: AnalogueValueType,
//    ) {
//        alConn[0].executeUpdate(
//            """
//                UPDATE MMS_agg_${sensorInfo.sensorId}
//                SET value_2 = $avgValue ,
//                    type_1 = ${valueType.value}
//                WHERE ontime = $avgOntime
//            """
//        )
//    }
//
////--- EQUIPMENT WORK SENSOR PART
//
//    private fun calcWork(sensorInfo: SensorInfo) {
//        val (alRawTime, alRawData) = startUpSensorDatas(sensorInfo)
//
//        val (oldTime, oldValue) = getLastWorkState(sensorInfo.sensorId)
//
//        var workBeginTime = if (oldValue == 1) {
//            oldTime ?: 0
//        } else {
//            0
//        }
//        var delayBeginTime = if (oldValue == 0) {
//            oldTime ?: 0
//        } else {
//            0
//        }
//
//        var curTime: Int? = null
//        for (pos in alRawTime.indices) {
//            curTime = alRawTime[pos]
//
//            val sensorData = AbstractObjectStateCalc.getSensorData(sensorInfo.portNum, alRawData[pos])?.toDouble() ?: continue
//
//            //--- ignore outbound values
//            if (ObjectCalc.isIgnoreSensorData(sensorInfo.minIgnore, sensorInfo.maxIgnore, sensorData)) {
//                continue
//            }
//
//            val sensorValue = ((sensorInfo.activeValue == 0) xor (sensorData > sensorInfo.boundValue))
//
//            //--- still in work?
//            if (sensorValue) {
//                //--- record of previous downtime
//                if (delayBeginTime != 0) {
//                    storeWorkState(sensorInfo.sensorId, delayBeginTime, curTime, 0, delayBeginTime == oldTime)
//                    delayBeginTime = 0
//                }
//                //--- beginning of work
//                if (workBeginTime == 0) {
//                    workBeginTime = curTime
//                }
//            } else {
//                //--- record of previous work
//                if (workBeginTime != 0) {
//                    storeWorkState(sensorInfo.sensorId, workBeginTime, curTime, 1, workBeginTime == oldTime)
//                    workBeginTime = 0
//                }
//                //--- start of downtime
//                if (delayBeginTime == 0) {
//                    delayBeginTime = curTime
//                }
//            }
//        }
//        //--- record of the last unclosed event
//        curTime?.let {
//            if (workBeginTime != 0) {
//                storeWorkState(sensorInfo.sensorId, workBeginTime, curTime, 1, workBeginTime == oldTime)
//            }
//            if (delayBeginTime != 0) {
//                storeWorkState(sensorInfo.sensorId, delayBeginTime, curTime, 0, delayBeginTime == oldTime)
//            }
//        }
//
//        curTime?.let {
//            refreshCalcTime(sensorInfo.sensorId, curTime)
//            AdvancedLogger.debug("Calc finished: new calc_time = ${DateTime_YMDHMS(zoneId, curTime)}")
//        }
//    }
//
//    private fun getLastWorkState(sensorId: Int): Pair<Int?, Int?> {
//        val rs = alConn[0].executeQuery(
//            """
//                SELECT ontime , type_1
//                FROM MMS_agg_${sensorId}
//                ORDER BY ontime DESC
//                LIMIT 1
//            """
//        )
//        val result = if (rs.next()) {
//            rs.getInt(1) to rs.getInt(2)
//        } else {
//            null to null
//        }
//        rs.close()
//
//        return result
//    }
//
//    private fun storeWorkState(sensorId: Int, begTime: Int, endTime: Int, value: Int, forUpdate: Boolean) {
//        alConn[0].executeUpdate(
//            if (forUpdate) {
//                """
//                    UPDATE MMS_agg_${sensorId}
//                    SET ontime_2 = $endTime
//                    WHERE ontime = $begTime
//                """
//            } else {
//                """
//                    INSERT INTO MMS_agg_${sensorId} ( ontime , ontime_2 , type_1 )
//                    VALUES ( $begTime , $endTime , $value )
//                """
//            }
//        )
//    }
//
//    /*
//            var prevSumValue = getLastAggregatedCounterValue(sensorInfo.sensorId)
//
//--
//
//                val storeValue = if (sensorInfo.isAbsoluteCount) {
//                    if (prevSumValue == null || sensorValue < prevSumValue) {
//                        Triple(curTime, 0.0, sensorValue)
//                    } else {
//                        Triple(curTime, sensorValue - prevSumValue, sensorValue)
//                    }
//                } else {
//                    Triple(curTime, sensorValue, 0.0)
//                }
//                prevSumValue = sensorValue
//
//                storeCounterData(sensorInfo.sensorId, storeValue)
//
//--------
//
//                if (sensorInfo.smoothTime == 0) {
//                    //--- get previous non-averaged data
//                    val (prevTime, prevValue) = getPrevData(sensorInfo, curTime, sensorValue)
//
//                    val valueType = if (sensorInfo.sensorType == SensorConfig.SENSOR_LIQUID_LEVEL) {
//                        getValueTypeForLiquidLevelSensors(sensorInfo, curTime, sensorValue, prevTime, prevValue)
//                    } else {
//                        getValueTypeForAnalogueSensors(sensorInfo, curTime, sensorValue, prevTime)
//                    }
//
//                    storeNonAveragedAnalogueData(sensorInfo, curTime, sensorValue, valueType)
//                }
//     */
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    class SensorInfo(
//        val sensorId: Int,
//        val objectId: Int,
//        val sensorType: Int,
//        val portNum: Int,
//        val calcTime: Int,
//        val minIgnore: Double,
//        val maxIgnore: Double,
//
//        // SensorConfig.SENSOR_MASS_ACCUMULATED, SensorConfig.SENSOR_VOLUME_ACCUMULATED, SensorConfig.SENSOR_LIQUID_USING,
//        // SensorConfig.SENSOR_ENERGO_COUNT_AD, SensorConfig.SENSOR_ENERGO_COUNT_AR,
//        // SensorConfig.SENSOR_ENERGO_COUNT_RD, SensorConfig.SENSOR_ENERGO_COUNT_RR,
//        val isAbsoluteCount: Boolean,
//
//        //SensorConfig.SENSOR_LIQUID_LEVEL, SensorConfig.SENSOR_WEIGHT,
//        //SensorConfig.SENSOR_TURN, SensorConfig.SENSOR_PRESSURE,
//        //SensorConfig.SENSOR_TEMPERATURE, SensorConfig.SENSOR_VOLTAGE,
//        //SensorConfig.SENSOR_POWER, SensorConfig.SENSOR_DENSITY,
//        //SensorConfig.SENSOR_MASS_FLOW, SensorConfig.SENSOR_VOLUME_FLOW
//        //SensorConfig.SENSOR_ENERGO_VOLTAGE, SensorConfig.SENSOR_ENERGO_CURRENT,
//        //SensorConfig.SENSOR_ENERGO_POWER_KOEF, SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
//        //SensorConfig.SENSOR_ENERGO_POWER_REACTIVE, SensorConfig.SENSOR_ENERGO_POWER_FULL
//        val analogMinLimit: Double, // except SensorConfig.SENSOR_LIQUID_LEVEL
//        val analogMaxLimit: Double, // except SensorConfig.SENSOR_LIQUID_LEVEL
//        val smoothTime: Int,
//        //SensorConfig.SENSOR_LIQUID_LEVEL
//        val detectIncKoef: Double,
//        val detectDecKoef: Double,
//        //SensorConfig.SENSOR_WORK
//        val boundValue: Double,
//        val activeValue: Int,
//    )
//}
//

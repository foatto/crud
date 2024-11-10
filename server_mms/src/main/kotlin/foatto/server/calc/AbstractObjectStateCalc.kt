package foatto.server.calc

import foatto.core.model.model.xy.GeoData
import foatto.server.model.SensorConfig
import foatto.server.util.AdvancedByteBuffer
import foatto.server.util.byteToHex

object AbstractObjectStateCalc {

    const val MAX_SPEED_AS_PARKING = 0

//--- UNUSED
//    fun getGeoData(bb: AdvancedByteBuffer): GeoData? {
//        var gd: GeoData? = null
//
//        while (bb.hasRemaining()) {
//            val (portNum, dataSize) = getSensorPortNumAndDataSize(bb)
//
//            if (portNum == SensorConfigGeo.PORT_NUM) {
//                gd = GeoData(bb.getInt(), bb.getInt(), bb.getShort().toInt(), bb.getInt())
//                break
//            } else {
//                bb.skip(dataSize)
//            }
//        }
//        bb.rewind()
//        return if (gd == null || gd.wgs.x == 0 && gd.wgs.y == 0) {
//            null
//        } else {
//            gd
//        }
//    }
//
//    --- чтение сырых данных в pulsar 2.0 вообще не используется ---
//    fun getSensorData(sensorPortNum: Int, bb: AdvancedByteBuffer): Number? {
//        var sensorValue: Number? = null
//
//        while (bb.hasRemaining()) {
//            val (portNum, dataSize) = getSensorPortNumAndDataSize(bb)
//
//            if (portNum == sensorPortNum) {
//                when (dataSize) {
//                    1 -> sensorValue = bb.getByte().toInt()
//                    2 -> sensorValue = bb.getShort().toInt() and 0xFFFF
//                    3 -> sensorValue = bb.getInt3()
//                    4 -> sensorValue = bb.getInt()
//                    8 -> sensorValue = bb.getDouble()
//                    else -> {
//                        if (dataSize < 0) {
//                            bb.getShortString() // пока неясно, как использовать
//                        }
//                    }
//                }
//                break
//            } else {
//                bb.skip(dataSize)
//            }
//        }
//        bb.rewind()
//        return sensorValue
//    }

    fun getSensorPortNumAndDataSize(bb: AdvancedByteBuffer): Pair<Int, Int> {
        val portNum = bb.getShort().toInt() and 0xFFFF
        val dataSize = bb.getShort() + 1
        return Pair(portNum, dataSize)
    }

    //--- define sensor data ignoring
    fun isIgnoreSensorData(minIgnore: Double?, maxIgnore: Double?, sensorData: Double?): Boolean =
        sensorData?.let {
            minIgnore?.let {
                maxIgnore?.let {
                    //--- classic variant: if minIgnore < maxIgnore, then ignore below minIgnore or above maxIgnore
                    if (minIgnore < maxIgnore) {
                        sensorData < minIgnore || sensorData > maxIgnore
                    } else {
                        //--- alternative: if minIgnore >= maxIgnore, then ignore between minIgnore and maxIgnore,
                        //--- given the case, if minIgnore == maxIgnore, then ignore nothing
                        sensorData < minIgnore && sensorData > maxIgnore
                    }
                } ?: run {
                    sensorData < minIgnore
                }
            } ?: run {
                maxIgnore?.let {
                    sensorData > maxIgnore
                } ?: false
            }
        } ?: true

    //--- calculation of the measured value by linear approximation
    fun getDataValue(sensorCalibration: List<Pair<Double, Double>>, sensorValue: Double): Double {
        var pos = -1

        if (sensorCalibration.isEmpty()) {
            return sensorValue
        }
        //--- if only one value is specified, use it as a usual "transforming" multiplier
        //--- (that is, it is a special case of two values, one of which is "0-> 0")
        if (sensorCalibration.size == 1) {
            val calibrationSensorValue = sensorCalibration[0].first
            val calibrationDataValue = sensorCalibration[0].second
            //--- meaningless one-to-one calibration or reduction to 0-th ADC, we can get division by 0, skip
            return if (calibrationSensorValue == calibrationDataValue || calibrationSensorValue == 0.0) {
                sensorValue
            } else {
                sensorValue / calibrationSensorValue * calibrationDataValue
            }
        }

        if (sensorValue < sensorCalibration[0].first) {
            pos = 0
        } else if (sensorValue > sensorCalibration[sensorCalibration.lastIndex].first) {
            pos = sensorCalibration.lastIndex - 1
        } else {
            for (i in 0 until sensorCalibration.size - 1) {
                if (sensorValue >= sensorCalibration[i].first && sensorValue <= sensorCalibration[i + 1].first) {
                    pos = i
                    break
                }
            }
        }

        return (sensorValue - sensorCalibration[pos].first) /
            (sensorCalibration[pos + 1].first - sensorCalibration[pos].first) *
            (sensorCalibration[pos + 1].second - sensorCalibration[pos].second) +
            sensorCalibration[pos].second
    }

    //--- defining the string form of sensor data
    fun getSensorString(aSensorType: Int?, dataSize: Int, bb: AdvancedByteBuffer): String {
        var sensorType = aSensorType
        val sensorValue: CharSequence

        if (sensorType == null) {
            sensorType = 0
        }

        when (sensorType) {
            SensorConfig.SENSOR_GEO -> {
                val gd = GeoData(bb.getInt(), bb.getInt(), bb.getShort().toInt(), bb.getInt())
                sensorValue = "x = ${gd.wgs.x}\ny = ${gd.wgs.y}\nскорость = ${gd.speed}\nпробег = ${gd.distance}"
            }

            else -> {
                when (dataSize) {
                    1 -> sensorValue = (bb.getByte().toInt() and 0xFF).toString()
                    2 -> sensorValue = (bb.getShort().toInt() and 0xFFFF).toString()
                    3 -> sensorValue = bb.getInt3().toString()
                    4 -> sensorValue = bb.getInt().toString()
                    8 -> sensorValue = bb.getDouble().toString()
                    else -> {
                        sensorValue = if (dataSize < 0) {
                            bb.getShortString()
                        } else {
                            val sb = StringBuilder()
                            for (i in 0 until dataSize) {
                                byteToHex(bb.getByte(), sb, false)
                            }
                            sb
                        }
                    }
                }
            }
        }

        return sensorValue.toString()
    }
}

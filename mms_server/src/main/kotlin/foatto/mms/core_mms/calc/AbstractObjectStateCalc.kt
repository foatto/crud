package foatto.mms.core_mms.calc

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.byteToHex
import foatto.mms.core_mms.GeoData
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigGeo

object AbstractObjectStateCalc {

    const val MAX_SPEED_AS_PARKING = 0

    fun getGeoData(scg: SensorConfigGeo, bb: AdvancedByteBuffer): GeoData? {
        var gd: GeoData? = null
        val sensorPortNum = scg.portNum

        while (bb.hasRemaining()) {
            val (portNum, dataSize) = getSensorPortNumAndDataSize(bb)

            if (portNum == sensorPortNum) {
                gd = GeoData(bb.getInt(), bb.getInt(), bb.getShort().toInt(), bb.getInt())
                break
            } else {
                bb.skip(dataSize)
            }
        }
        bb.rewind()
        return if (gd == null || gd.wgs.x == 0 && gd.wgs.y == 0) {
            null
        } else {
            gd
        }
    }

    fun getSensorData(sensorPortNum: Int, bb: AdvancedByteBuffer): Number? {
        var sensorValue: Number? = null

        while (bb.hasRemaining()) {
            val (portNum, dataSize) = getSensorPortNumAndDataSize(bb)

            if (portNum == sensorPortNum) {
                when (dataSize) {
                    1 -> sensorValue = bb.getByte().toInt()
                    2 -> sensorValue = bb.getShort().toInt() and 0xFFFF
                    3 -> sensorValue = bb.getInt3()
                    4 -> sensorValue = bb.getInt()
                    8 -> sensorValue = bb.getDouble()
                }
                break
            } else {
                bb.skip(dataSize)
            }
        }
        bb.rewind()
        return sensorValue
    }

    fun getSensorPortNumAndDataSize(bb: AdvancedByteBuffer): Pair<Int, Int> {
        val portNum = bb.getShort().toInt() and 0xFFFF
        val dataSize = (bb.getShort().toInt() and 0xFFFF) + 1
        return Pair(portNum, dataSize)
    }

    //--- calculation of the measured value by linear approximation
    fun getSensorValue(alValueSensor: List<Double>?, alValueData: List<Double>?, sensorValue: Double): Double {
        var pos = -1

        if (alValueSensor.isNullOrEmpty() || alValueData.isNullOrEmpty()) return sensorValue
        //--- if only one value is specified, use it as a usual "transforming" multiplier
        //--- (that is, it is a special case of two values, one of which is "0-> 0")
        if (alValueSensor.size == 1 || alValueData.size == 1) {
            //--- meaningless one-to-one calibration or reduction to 0-th ADC, we can get division by 0, skip
            return if (alValueSensor[0] == alValueData[0] || alValueSensor[0] == 0.0) {
                sensorValue
            } else {
                sensorValue / alValueSensor[0] * alValueData[0]
            }
        }

        if (sensorValue < alValueSensor[0]) {
            pos = 0
        } else if (sensorValue > alValueSensor[alValueSensor.size - 1]) {
            pos = alValueSensor.size - 2
        } else {
            for (i in 0 until alValueSensor.size - 1) {
                if (sensorValue >= alValueSensor[i] && sensorValue <= alValueSensor[i + 1]) {
                    pos = i
                    break
                }
            }
        }

        return (sensorValue - alValueSensor[pos]) / (alValueSensor[pos + 1] - alValueSensor[pos]) * (alValueData[pos + 1] - alValueData[pos]) + alValueData[pos]
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
                        sensorValue = StringBuilder()
                        for (i in 0 until dataSize) byteToHex(bb.getByte(), sensorValue, false)
                    }
                }
            }
        }

        return sensorValue.toString()
    }
}

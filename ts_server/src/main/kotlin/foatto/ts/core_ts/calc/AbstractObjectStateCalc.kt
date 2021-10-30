package foatto.ts.core_ts.calc

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.byteToHex

object AbstractObjectStateCalc {

    fun getSensorData(sensorPortNum: Int, bb: AdvancedByteBuffer): Number? {
        var sensorValue: Number? = null

        while (bb.hasRemaining()) {
            val (portNum, dataSize) = getSensorPortNumAndDataSize(bb)

            if (portNum == sensorPortNum) {
                when (dataSize) {
                    1 -> sensorValue = bb.getByte().toInt() and 0xFF
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

        if (alValueSensor.isNullOrEmpty() || alValueData.isNullOrEmpty()) {
            return sensorValue
        }
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
    fun getSensorString(aSensorType: Int?, dataSize: Int, bb: AdvancedByteBuffer): String =
        when (dataSize) {
            1 -> (bb.getByte().toInt() and 0xFF).toString()
            2 -> (bb.getShort().toInt() and 0xFFFF).toString()
            3 -> bb.getInt3().toString()
            4 -> bb.getInt().toString()
            8 -> bb.getDouble().toString()
            else -> {
                val sensorValue = StringBuilder()
                for (i in 0 until dataSize) {
                    byteToHex(bb.getByte(), sensorValue, false)
                }
                sensorValue
            }
        }.toString()
}

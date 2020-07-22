package foatto.mms.core_mms.calc

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.byteToHex
import foatto.mms.core_mms.GeoData
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.sensor.SensorConfig

object AbstractObjectStateCalc {

    const val MAX_SPEED_AS_PARKING = 0

    fun getGeoData( oc: ObjectConfig, bb: AdvancedByteBuffer ): GeoData? {
        var gd: GeoData? = null
        val sensorPortNum = oc.scg!!.portNum

        while( bb.hasRemaining() ) {
            val ( portNum, dataSize ) = getSensorPortNumAndDataSize( oc, bb )

            if( portNum == sensorPortNum ) {
                gd = GeoData( bb.getInt(), bb.getInt(), bb.getShort().toInt(), bb.getInt() )
                break
            }
            else bb.skip( dataSize )//--- пропуск нужного кол-ва байт
        }
        bb.rewind()    // подготовка для следующего использования
        //--- данные с ошибкой GPS бессмысленны
        return if( gd == null || gd.wgs.x == 0 && gd.wgs.y == 0 ) null else gd
    }

    fun getSensorData( oc: ObjectConfig, sensorPortNum: Int, bb: AdvancedByteBuffer ): Number? {
        var sensorValue: Number? = null

        while( bb.hasRemaining() ) {
            val ( portNum, dataSize ) = getSensorPortNumAndDataSize( oc, bb )

            if( portNum == sensorPortNum ) {
                when( dataSize ) {
                    1 -> sensorValue = bb.getByte().toInt() and 0xFF
                    2 -> sensorValue = bb.getShort().toInt() and 0xFFFF
                    3 -> sensorValue = bb.getInt3()
                    4 -> sensorValue = bb.getInt()
                    8 -> sensorValue = bb.getDouble()
                }
                break
            }
            else bb.skip( dataSize )//--- пропуск нужного кол-ва байт
        }
        bb.rewind()    // подготовка для следующего использования
        return sensorValue
    }

    //--- определение номера порта и кол-ва данных по версии данных
    fun getSensorPortNumAndDataSize( oc: ObjectConfig, bb: AdvancedByteBuffer ): Pair<Int,Int> {
        val portNum = if( oc.dataVersion == 0 ) bb.getByte().toInt() and 0xFF else bb.getShort().toInt() and 0xFFFF
        //--- значения 0..255 или 0..65535 дают нам размер будущих данных от 1 до 256 байт или от 1 до 65536 байт
        val dataSize = ( if( oc.dataVersion == 0 ) bb.getByte().toInt() and 0xFF else bb.getShort().toInt() and 0xFFFF ) + 1
        return Pair( portNum, dataSize )
    }

    //--- вычисление значения измеряемой величины путем линейной аппроксимации
    fun getSensorValue( alValueSensor: List<Double>?, alValueData: List<Double>?, sensorValue: Double ): Double {
        var pos = -1

        if( alValueSensor.isNullOrEmpty() || alValueData.isNullOrEmpty() ) return sensorValue
        //--- если задано только одно значение, используем как обычный "преобразующий" множитель
        //--- ( т.е. является частным случаем двух значений, одно из которых "0->0" )
        if( alValueSensor.size == 1 || alValueData.size == 1 ) {
            //--- бессмысленная тарировка один к одному или приведение к 0-му АЦП, можем получить деление на 0, пропускаем
            return if( alValueSensor[ 0 ] == alValueData[ 0 ] || alValueSensor[ 0 ] == 0.0 ) sensorValue
            else sensorValue / alValueSensor[ 0 ] * alValueData[ 0 ]
        }

        if( sensorValue < alValueSensor[ 0 ] ) pos = 0
        else if( sensorValue > alValueSensor[ alValueSensor.size - 1 ] ) pos = alValueSensor.size - 2
        else for( i in 0 until alValueSensor.size - 1 )
            if( sensorValue >= alValueSensor[ i ] && sensorValue <= alValueSensor[ i + 1 ] ) {
                pos = i
                break
            }

        return ( sensorValue - alValueSensor[ pos ] ) / ( alValueSensor[ pos + 1 ] - alValueSensor[ pos ] ) * ( alValueData[ pos + 1 ] - alValueData[ pos ] ) + alValueData[ pos ]
    }

    //--- определение строковой формы данных датчика
    fun getSensorString( aSensorType: Int?, dataSize: Int, bb: AdvancedByteBuffer ): String {
        var sensorType = aSensorType
        //--- по каждому номеру порта - составляем визуальное представление значения
        val sensorValue: CharSequence

        //--- исправляем входящий null здесь, дабы не переусложнять логику
        if( sensorType == null ) sensorType = 0

        when( sensorType ) {
            SensorConfig.SENSOR_GEO -> {
                val gd = GeoData( bb.getInt(), bb.getInt(), bb.getShort().toInt(), bb.getInt() )
                sensorValue = "x = ${gd.wgs.x}\ny = ${gd.wgs.y}\nскорость = ${gd.speed}\nпробег = ${gd.distance}"
            }

            else -> if( dataSize == 1 ) sensorValue = ( bb.getByte().toInt() and 0xFF ).toString()
                    else if( dataSize == 2 ) sensorValue = ( bb.getShort().toInt() and 0xFFFF ).toString()
                    else if( dataSize == 3 ) sensorValue = bb.getInt3().toString()
                    else if( dataSize == 4 ) sensorValue = bb.getInt().toString()
                    else if( dataSize == 8 ) sensorValue = bb.getDouble().toString()
                    else {
                        sensorValue = StringBuilder()
                        for( i in 0 until dataSize ) byteToHex( bb.getByte(), sensorValue, false )
                    }
        }

        return sensorValue.toString()
    }
}

package foatto.fs.core_fs.calc

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.getDateTimeInt
import foatto.core.util.getZoneId
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

private val zoneId0 = getZoneId( 0 )

class Parser_F101_04 : MeasureParser {
    val PAGE_SIZE = 528

    override fun getEndian(): ByteOrder = ByteOrder.BIG_ENDIAN

    override fun parseData(bbData: AdvancedByteBuffer, alData: MutableList<SensorData> ): String {
        if( alData.size != 6 ) {
            AdvancedLogger.error( "Sensor list not equal 6 = ${alData.size}" )
            return "\nF101_04: Кол-во датчиков не равно 6 = ${bbData.remaining()}"
        }
        else if( bbData.remaining() < PAGE_SIZE * 2 ) {
            AdvancedLogger.error( "Not enough data: bbData.remaining() = ${bbData.remaining()}" )
            return "\nF101_04: недостаточно данных = ${bbData.remaining()}"
        }
        else {
            //--- пропускаем 0-ую страницу
            bbData.skip( PAGE_SIZE )
var pageNo = 1
            //--- далее обрабатываем только если достаточно данных
            while( bbData.remaining() >= PAGE_SIZE ) {
AdvancedLogger.debug( "pageNo = $pageNo" )
pageNo++

                val mo = bbData.getByte().toInt() and 0xFF
                val da = bbData.getByte().toInt() and 0xFF
                val ho = bbData.getByte().toInt() and 0xFF
                val mi = bbData.getByte().toInt() and 0xFF
                val se = bbData.getByte().toInt() and 0xFF
                val ye = 2000 + ( bbData.getByte().toInt() and 0xFF)  // Вовчикова самодеятельность - год вместо "_200mS"
AdvancedLogger.debug( "ye = $ye" )
AdvancedLogger.debug( "mo = $mo" )
AdvancedLogger.debug( "da = $da" )
AdvancedLogger.debug( "ho = $ho" )
AdvancedLogger.debug( "mi = $mi" )
AdvancedLogger.debug( "se = $se" )

                val sensorMask = bbData.getByte().toInt() and 0xFF
                val interval = bbData.getShort().toInt() and 0xFFFF
                val bytesPerPacket = bbData.getByte().toInt() and 0xFF
                val powerVoltage = ( bbData.getByte().toInt() and 0xFF ) * 0.1
                bbData.getByte()    // SKIP page sum
AdvancedLogger.debug( "sensorMask = $sensorMask" )
AdvancedLogger.debug( "interval = $interval" )
AdvancedLogger.debug( "bytesPerPacket = $bytesPerPacket" )
AdvancedLogger.debug( "powerVoltage = $powerVoltage" )
                
                if( interval == 0 ) return "\nИнтервал замеров = 0"
                if( bytesPerPacket == 0 ) return "\nРазмер замера = 0"

                //--- разбор маски записываемых датчиков
                val isGalvanicHumidity = ( sensorMask and 0x01 ) != 0
                val isPressureTemp = ( sensorMask and 0x02 ) != 0
                val isCapacityHumidity = ( sensorMask and 0x04 ) != 0
                val isTemperature = ( sensorMask and 0x08 ) != 0
                val isPressure = ( sensorMask and 0x10 ) != 0
AdvancedLogger.debug( "isGalvanicHumidity = $isGalvanicHumidity" )
AdvancedLogger.debug( "isPressureTemp = $isPressureTemp" )
AdvancedLogger.debug( "isCapacityHumidity = $isCapacityHumidity" )
AdvancedLogger.debug( "isTemperature = $isTemperature" )
AdvancedLogger.debug( "isPressure = $isPressure" )

                //--- сколько пакетов данных на странице
                val packetCount = ( PAGE_SIZE - 12 ) / bytesPerPacket
                //--- размер нечитаемого остатка страницы
                val lastSkipBytes = PAGE_SIZE - 12 - packetCount * bytesPerPacket
AdvancedLogger.debug( "packetCount = $packetCount" )
AdvancedLogger.debug( "lastSkipBytes = $lastSkipBytes" )

//                val today = GregorianCalendar()
//                val ye = if( mo <= today.get( GregorianCalendar.MONTH ) + 1 ) today.get( GregorianCalendar.YEAR ) else today.get( GregorianCalendar.YEAR ) - 1
                var time = getDateTimeInt(zoneId0, intArrayOf( ye, mo, da, ho, mi, se ))

                alData[ 5 ].alMeasureData.add( Pair( time, powerVoltage ) )

                for( i in 0 until packetCount ) {
                    //--- несмотря на то, что список датчиков уже был в заголовке, надо каждый тип рассматривать отдельно, т.к. они пишутся/читаются по разному/разным кол-вом байт
                    if( isPressure ) {
                        val value = bbData.getShort()
                        if( value != 0xFFFF.toShort() )
                            alData[ 0 ].alMeasureData.add( Pair( time, getSensorValue( alData[ 0 ].alCalibration, ( value.toInt() and 0xFFFF ).toDouble() ) ) )
                    }
                    if( isTemperature ) {
                        val value = bbData.getShort()
                        if( value != 0xFFFF.toShort() )
                            alData[ 1 ].alMeasureData.add( Pair( time, getSensorValue( alData[ 1 ].alCalibration, ( value.toInt() /*and 0xFFFF*/ ).toDouble() ) ) )
                    }
                    if( isCapacityHumidity ) {
                        val value = bbData.getShort()
                        if( value != 0xFFFF.toShort() )
                            alData[ 2 ].alMeasureData.add( Pair( time, getSensorValue( alData[ 2 ].alCalibration, ( value.toInt() and 0xFFFF ).toDouble() ) ) )
                    }
                    if( isPressureTemp ) {
                        val value = bbData.getShort()
                        if( value != 0xFFFF.toShort() )
                            alData[ 3 ].alMeasureData.add( Pair( time, getSensorValue( alData[ 3 ].alCalibration, ( value.toInt() and 0xFFFF ).toDouble() ) ) )
                    }
                    if( isGalvanicHumidity ) {
                        val value = bbData.getByte()
                        if( value != 0xFF.toByte() )
                            alData[ 4 ].alMeasureData.add( Pair( time, getSensorValue( alData[ 4 ].alCalibration, ( value.toInt() and 0xFF ).toDouble() ) ) )
                    }
                    time += interval
                }
                bbData.skip( lastSkipBytes )
            }
            //--- вычисление min/max Time/Value
            for( sensorData in alData ) {
                sensorData.minTime = sensorData.alMeasureData.firstOrNull()?.first ?: 0
                sensorData.maxTime = sensorData.alMeasureData.lastOrNull()?.first ?: 0
                for( measureData in sensorData.alMeasureData ) {
                    sensorData.minValue = min( sensorData.minValue, measureData.second )
                    sensorData.maxValue = max( sensorData.maxValue, measureData.second )
                }
                //--- во избежание зависания графиков - если minValue == maxValue, то исправим ситуацию
                if( sensorData.maxValue <= sensorData.minValue ) sensorData.maxValue = sensorData.minValue + 1
            }
        }
        return ""
    }
}


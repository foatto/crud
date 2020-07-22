package foatto.fs.core_fs.calc

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.fs.core_fs.device.mDevice.Companion.DEVICE_TYPE_F101_04
import java.io.File
import java.nio.ByteOrder
import java.util.*

//--------------------------------------------------------------------------------------------

fun getMeasureFile( rootDirName: String, measureID: Int ) = File( "$rootDirName/foton_data/$measureID" )

//--------------------------------------------------------------------------------------------

val SENSOR_TYPE_P1 = 101
val SENSOR_TYPE_P2 = 102
val SENSOR_TYPE_T1 = 103
val SENSOR_TYPE_T2 = 104
val SENSOR_TYPE_R1 = 105
val SENSOR_TYPE_R2 = 106
val SENSOR_TYPE_VOLTAGE = 107
val SENSOR_TYPE_STATUS = 108
val SENSOR_TYPE_FLOW = 109
val SENSOR_TYPE_FLOW_SUM = 110
val SENSOR_TYPE_CLEAN_DATA = 111

fun getSensorDescrMap(): TreeMap<Int,String> {
    val tmSensorType = TreeMap<Int,String>()

    tmSensorType[ SENSOR_TYPE_P1 ] = "Давление 1"
    tmSensorType[ SENSOR_TYPE_P2 ] = "Давление 2"
    tmSensorType[ SENSOR_TYPE_T1 ] = "Температура 1"
    tmSensorType[ SENSOR_TYPE_T2 ] = "Температура 2"
    tmSensorType[ SENSOR_TYPE_R1 ] = "Резистивиметр 1"
    tmSensorType[ SENSOR_TYPE_R2 ] = "Резистивиметр 2"
    tmSensorType[ SENSOR_TYPE_VOLTAGE ] = "Напряжение"
    tmSensorType[ SENSOR_TYPE_STATUS ] = "Статус"
    tmSensorType[ SENSOR_TYPE_FLOW ] = "Расход жидкости"
    tmSensorType[ SENSOR_TYPE_FLOW_SUM ] = "Накопленный расход"
    tmSensorType[ SENSOR_TYPE_CLEAN_DATA ] = "АЦП"

    return tmSensorType
}

//--------------------------------------------------------------------------------------------

val DIM_NONE = 0
val DIM_Atm = 1
val DIM_MPa = 2
val DIM_C = 3
val DIM_PERCENT = 4
val DIM_mV = 5
val DIM_V = 6
val DIM_M3H = 7
val DIM_M3D = 8
val DIM_LITER = 9
val DIM_M3 = 10

fun getDimDescrMap(): TreeMap<Int,String> {
    val tmDimType = TreeMap<Int,String>()

    tmDimType[ DIM_NONE ] = "-"
    tmDimType[ DIM_Atm ] = "атм."
    tmDimType[ DIM_MPa ] = "МПа"
    tmDimType[ DIM_C ] = "С"
    tmDimType[ DIM_PERCENT ] = "%"
    tmDimType[ DIM_mV ] = "мВ"
    tmDimType[ DIM_V ] = "В"
    tmDimType[ DIM_M3H ] = "куб.м/час"
    tmDimType[ DIM_M3D ] = "куб.м/сутки"
    tmDimType[ DIM_LITER ] = "л"
    tmDimType[ DIM_M3 ] = "куб.м"

    return tmDimType
}

//--------------------------------------------------------------------------------------------

class SensorData( val typeID: Int, val typeDescr: String, val dimDescr: String ) {
    val alCalibration = mutableListOf<Pair<Double,Double>>()

    var minTime = 0
    var maxTime = 0
    var minValue = 0.0    // таки лучше хотя бы 0 в минимуме, чем + Double.MAX_VALUE, особенно когда min == max
    var maxValue = - Double.MAX_VALUE
    val alMeasureData = mutableListOf<Pair<Int,Double>>()
}

//--------------------------------------------------------------------------------------------

interface MeasureParser {
    fun getEndian(): ByteOrder
    fun parseData(bbData: AdvancedByteBuffer, alData: MutableList<SensorData> ): String

    fun getSensorValue( alCalibration: List<Pair<Double,Double>>, sensorValue: Double ): Double {
        var pos = -1
        //--- если в таблице калибровки вообще не задано значений, то возвращаем значение датчика "как есть"
        if( alCalibration.isNullOrEmpty() ) return sensorValue
        //--- если задано только одно значение, используем как обычный "преобразующий" множитель
        //--- ( т.е. является частным случаем двух значений, одно из которых "0->0" )
        if( alCalibration.size == 1 ) {
            //--- бессмысленная тарировка один к одному или приведение к 0-му АЦП, можем получить деление на 0, пропускаем
            return if( alCalibration[ 0 ].first == alCalibration[ 0 ].second || alCalibration[ 0 ].first == 0.0 ) sensorValue
            else sensorValue / alCalibration[ 0 ].first * alCalibration[ 0 ].second
        }

        if( sensorValue < alCalibration[ 0 ].first ) pos = 0
        else if( sensorValue > alCalibration.last().first ) pos = alCalibration.size - 2
        else for( i in 0 until alCalibration.size - 1 )
            if( sensorValue >= alCalibration[ i ].first && sensorValue <= alCalibration[ i + 1 ].first ) {
                pos = i
                break
            }

        //--- ошибочная тарировка: два совпадающих значения АЦП
        if( ( alCalibration[ pos + 1 ].first - alCalibration[ pos ].first ) == 0.0 ) return sensorValue

        return ( sensorValue - alCalibration[ pos ].first ) /
               ( alCalibration[ pos + 1 ].first - alCalibration[ pos ].first ) *
               ( alCalibration[ pos + 1 ].second - alCalibration[ pos ].second ) + alCalibration[ pos ].second
    }

}

//--------------------------------------------------------------------------------------------

class MeasureCalc( rootDirName: String, measureID: Int, withDataParsing: Boolean ) {

    var errorDescr = ""

    val alSensor = mutableListOf<SensorData>()

    init {
        val fileData = getMeasureFile( rootDirName, measureID )

        val tmSensorDescr = getSensorDescrMap()
        val tmDimDescr = getDimDescrMap()

AdvancedLogger.debug( "--- measure = $measureID ---" )

        if( fileData.exists() ) {
            val bbFile = AdvancedByteBuffer( fileData.readBytes(), ByteOrder.LITTLE_ENDIAN )
            val fileKey = bbFile.getInt()
            if( fileKey == 0x11223344 ) {
                bbFile.getInt() // SKIP nextElementAddr
                bbFile.getInt() // SKIP curElementSize
                bbFile.getInt() // SKIP fileNo
                val fileVersion = bbFile.getInt()
                if( fileVersion == 0x00010002 ) {
                    bbFile.getInt() // SKIP prevFileAddr
                    bbFile.getInt() // SKIP nextFileAddr
                    bbFile.getInt() // SKIP timeFileWriteBeg
                    bbFile.getInt() // SKIP fileSize

                    bbFile.getInt() // SKIP controllerVersion
                    bbFile.getInt() // SKIP controllerSerialNo
                    bbFile.getInt() // SKIP controllerStatus

                    val deviceType = bbFile.getInt()
                    bbFile.getInt() // SKIP deviceAddr
                    bbFile.getInt() // SKIP deviceSerial

                    bbFile.getInt() // SKIP deviceVersion
                    bbFile.getInt() // SKIP deviceSpec

                    val sensorCount = bbFile.getShort().toInt() and 0xFFFF
                    for( i in 0 until sensorCount ) {
                        val sensorType = bbFile.getShort().toInt() and 0xFFFF
                        val sensorDim = bbFile.getShort().toInt() and 0xFFFF
                        val sensor = SensorData( sensorType,
                                                 tmSensorDescr[ sensorType ] ?: "Неизвестный тип датчика [$sensorType]",
                                                 tmDimDescr[ sensorDim ] ?: "Неизвестная ед. изм. [$sensorDim]")

                        val calibCount = bbFile.getShort().toInt() and 0xFFFF
                        for( j in 0 until calibCount )
                            sensor.alCalibration.add( Pair( bbFile.getInt().toDouble(), bbFile.getInt().toDouble() ) )
                        alSensor.add( sensor )
                    }

                    if( withDataParsing ) {
                        val parser: MeasureParser? =
                            when( deviceType ) {
                                DEVICE_TYPE_F101_04 -> Parser_F101_04()
                                //--- такого быть не должно
                                else -> {
                                    AdvancedLogger.error( "Unknown device type = $deviceType" )
                                    errorDescr += "\nНеизвестный тип прибора = $deviceType"
                                    null
                                }
                            }
                        parser?.let {
                            //--- отдельный буфер для чистых данных (размер взят с небольшим запасом)
                            val bbData = AdvancedByteBuffer( bbFile.remaining(), parser.getEndian() )

                            while( bbFile.remaining() >= 16 ) {
                                val dataKey = bbFile.getInt()
                                if( dataKey != 0x11223344 ) {
                                    AdvancedLogger.error( "Wrong data key = $dataKey" )
                                    errorDescr += "\nНеправильный ключ данных = $dataKey"
                                    break
                                }

                                bbFile.getInt() // SKIP nextElementAddr
                                val elementSize = bbFile.getInt() - 16  // вычитаем размер шапки
                                bbFile.getInt() // SKIP fileNo

                                AdvancedLogger.debug( "ElementSize - 16 = $elementSize" )

                                if( elementSize <= 0 ) {
                                    AdvancedLogger.error( "ElementSize too small = $elementSize" )
                                    errorDescr += "\nСлишком малый размер элемента = $elementSize"
                                    break
                                }

                                if( elementSize <= bbFile.remaining() )
                                    bbData.put( bbFile.get( elementSize ) )
                                //--- данные могут быть ещё не докачаны
                                else {
                                    AdvancedLogger.error( "elementSize > bb.remaining() = $elementSize > ${bbFile.remaining()}" )
                                    errorDescr += "\nРазмер элемента больше количества данных = $elementSize > ${bbFile.remaining()}"
                                    break
                                }
                            }

                            //--- начинаем разбор чистых данных с прибора
                            bbData.flip()
                            errorDescr += parser.parseData( bbData, alSensor )
                        }
                    }
                }
                else {
                    AdvancedLogger.error( "Unsupported file version = $fileVersion" )
                    errorDescr += "\nНеподдерживаемая версия файла = $fileVersion"
                }
            }
            else {
                AdvancedLogger.error( "Wrong file key = $fileKey" )
                errorDescr += "\nНеправильный ключ файла = $fileKey"
            }
        }
        else {
            AdvancedLogger.error( "File not found = ${fileData.canonicalPath}" )
            errorDescr += "\nНе найден файл с данными = ${fileData.canonicalPath}"
        }
    }
}

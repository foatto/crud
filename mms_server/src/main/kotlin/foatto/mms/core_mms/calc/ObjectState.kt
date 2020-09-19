package foatto.mms.core_mms.calc

import foatto.core.app.xy.XyProjection
import foatto.core.app.xy.geom.XyPoint
import foatto.core.util.getAngle
import foatto.core.util.getCurrentTimeInt
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.sensor.SensorConfig
import foatto.mms.core_mms.sensor.SensorConfigAnalogue
import foatto.mms.core_mms.sensor.SensorConfigSignal
import foatto.mms.core_mms.sensor.SensorConfigWork
import foatto.sql.CoreAdvancedStatement
import java.nio.ByteOrder
import java.util.*

class ObjectState {

    var time = MAX_TIME
    var pixPoint: XyPoint? = null
    var speed = 0
    var objectAngle: Double? = null

    var tmSignalState = TreeMap<String, Boolean>()
    var tmWorkState = TreeMap<String, Boolean>()
    var tmLiquidError = TreeMap<String, String>()
    var tmLiquidLevel = TreeMap<String, Double>()
    var tmLiquidDim = TreeMap<String, String>()

    companion object {

        //--- если нормальных точек не найдётся, то пусть время будет из далёкого будущего,
        //--- чтобы неопределённое положение на карте не светилось
        //--- ( а чтобы не вылезала ошибка переполнения целого
        //--- при задании начального времени как Long.MAX_VALUE / 1000,
        //--- сделаем максимум как Integer.MAX_VALUE )
        private val MAX_TIME = Integer.MAX_VALUE

        //--- работаем только с активной базой
        fun getState(stm: CoreAdvancedStatement, oc: ObjectConfig ): ObjectState {

            val result = ObjectState()

            //--- датчики сигналов
            val hmSCS = oc.hmSensorConfig[ SensorConfig.SENSOR_SIGNAL ]
            //--- датчики работы оборудования
            val hmSCW = oc.hmSensorConfig[ SensorConfig.SENSOR_WORK ]
            //--- датчики уровня жидкости
            val hmSCLL = oc.hmSensorConfig[ SensorConfig.SENSOR_LIQUID_LEVEL ]

            //--- будем ползти назад по времени увеличивающимся шагом LIMIT,
            //--- пока не найдём хоть какое-то последнее положение
            var lastTime = Integer.MAX_VALUE
            //--- 2 последние точки могут быть одинаковыми с петролайн-приборов ( линейная точка+CUR_COORDS ),
            //--- поэтому начнём запрашивать сразу с 4-х последних точек
            var lastLimit = 4
            while( true ) {
                //--- флаг наличия данных
                var isDataExist = false
                val inRs = stm.executeQuery(
                    " SELECT ${stm.getPreLimit( lastLimit )} ontime , sensor_data " +
                    " FROM MMS_data_${oc.objectID} " +
                    " WHERE ontime < $lastTime ${stm.getMidLimit( lastLimit )} " +
                    " ORDER BY ontime DESC ${stm.getPostLimit( lastLimit )} " )
                while( inRs.next() ) {
                    //--- данные вообще есть, ещё есть смысл крутиться дальше
                    isDataExist = true

                    lastTime = inRs.getInt( 1 )
                    val bbSensor = inRs.getByteBuffer( 2, ByteOrder.BIG_ENDIAN )

                    //--- один раз запоминаем последнее время
                    if( result.time == MAX_TIME ) result.time = lastTime

                    //--- если прописан гео-датчик и ещё не все данные определены
                    if( oc.scg != null && ( result.pixPoint == null || result.objectAngle == null ) ) {
                        val gd = AbstractObjectStateCalc.getGeoData( oc, bbSensor )
                        //--- самих геоданных в этой строке может и не оказаться
                        if( gd != null ) {
                            //--- сбор данных по последней/основной точке
                            if( result.pixPoint == null ) {
                                result.pixPoint = XyProjection.wgs_pix( gd.wgs )
                                if( oc.scg!!.isUseSpeed ) result.speed = gd.speed
                            }
                            //--- вычисление недостающего угла поворота а/м
                            //--- ( только после того, как будет найдена предыдущая последняя точка )
                            else if( result.objectAngle == null ) {
                                val prjPointPrev = XyProjection.wgs_pix( gd.wgs )
                                //--- текущий угол меняем если только координаты полностью сменились,
                                //--- иначе при совпадающих старых/новых координатах ( т.е. стоянии на месте )
                                //--- получим всегда 0-й угол, что некрасиво
                                if( result.pixPoint!!.x != prjPointPrev.x || result.pixPoint!!.y != prjPointPrev.y )
                                //--- угол меняет знак, т.к. на экране ось Y идет сверху вниз
                                    result.objectAngle = if( result.pixPoint!!.x == prjPointPrev.x ) ( if( result.pixPoint!!.y > prjPointPrev.y ) 90.0 else -90.0 )
                                                         else getAngle( ( result.pixPoint!!.x - prjPointPrev.x ).toDouble(), ( result.pixPoint!!.y - prjPointPrev.y ).toDouble() )
                            }
                        }
                    }
                    //--- если прописаны датчики сигналов
                    if( hmSCS != null && hmSCS.size != result.tmSignalState.size ) {
                        for( portNum in hmSCS.keys ) {
                            val scs = hmSCS[portNum] as SensorConfigSignal
                            if( result.tmSignalState[ scs.descr ] == null ) {
                                val sensorData = AbstractObjectStateCalc.getSensorData( oc, scs.portNum, bbSensor )?.toInt() ?: 0
                                //--- вручную игнорируем заграничные значения
                                //--- новое условие - не академически/бесполезно ИГНОРИРУЕМ,
                                //--- а считаем, что оборудование вне заданных границ НЕ РАБОТАЕТ
                                //if(  sensorData < scw.minIgnore || sensorData > scw.maxIgnore  ) continue;
                                val curValue = sensorData >= scs.minIgnore && sensorData <= scs.maxIgnore && ( scs.activeValue == 0 ) xor ( sensorData > scs.boundValue )
                                result.tmSignalState[ scs.descr ] = curValue
                            }
                        }
                    }
                    //--- если прописаны датчики работы оборудования
                    if( hmSCW != null && hmSCW.size != result.tmWorkState.size ) {
                        for( portNum in hmSCW.keys ) {
                            val scw = hmSCW[portNum] as SensorConfigWork
                            if( result.tmWorkState[ scw.descr ] == null ) {
                                val sensorData = AbstractObjectStateCalc.getSensorData( oc, scw.portNum, bbSensor )?.toInt() ?: 0
                                //--- вручную игнорируем заграничные значения
                                //--- новое условие - не академически/бесполезно ИГНОРИРУЕМ,
                                //--- а считаем, что оборудование вне заданных границ НЕ РАБОТАЕТ
                                //if(  sensorData < scw.minIgnore || sensorData > scw.maxIgnore  ) continue;
                                var curValue = sensorData >= scw.minIgnore && sensorData <= scw.maxIgnore && ( scw.activeValue == 0 ) xor ( sensorData > scw.boundValue )

                                //--- учёт модификатора работы датчика - учёт работы только в движении или только на стоянке
                                if( curValue && oc.scg != null && oc.scg!!.isUseSpeed && result.pixPoint != null ) {
                                    if( scw.calcInMoving xor scw.calcInParking )
                                        curValue = ( if( result.speed > AbstractObjectStateCalc.MAX_SPEED_AS_PARKING ) scw.calcInMoving else scw.calcInParking )
                                    //--- если обе галочки выключены - датчик никогда не считается
                                    else curValue = curValue and ( scw.calcInMoving && scw.calcInParking )
                                }
                                result.tmWorkState[ scw.descr ] = curValue
                            }
                        }
                    }
                    //--- если прописаны датчики уровня жидкости
                    if( hmSCLL != null && hmSCLL.size != result.tmLiquidLevel.size ) {
                        for( portNum in hmSCLL.keys ) {
                            val sca = hmSCLL[portNum] as SensorConfigAnalogue
                            if( result.tmLiquidLevel[ sca.descr ] == null ) {
                                //--- ручной разбор сырых данных
                                val sensorData = AbstractObjectStateCalc.getSensorData( oc, sca.portNum, bbSensor )?.toInt() ?: 0
                                //--- проверка на ошибку
                                val troubleDescr = SensorConfigAnalogue.hmLLErrorCodeDescr[sensorData]
                                //--- если есть ошибка и она уже достаточное время
                                if (troubleDescr != null && getCurrentTimeInt() - result.time > SensorConfigAnalogue.hmLLMinSensorErrorTime[sensorData]!!) {
                                    result.tmLiquidError[sca.descr] = troubleDescr
                                    //--- значение не важно, ибо ошибка, лишь бы что-то было
                                    result.tmLiquidLevel[sca.descr] = 0.0
                                    result.tmLiquidDim[sca.descr] = sca.dim
                                } else if (sensorData >= sca.minIgnore && sensorData <= sca.maxIgnore) {
                                    result.tmLiquidLevel[sca.descr] = AbstractObjectStateCalc.getSensorValue(sca.alValueSensor, sca.alValueData, sensorData.toDouble())
                                    result.tmLiquidDim[sca.descr] = sca.dim
                                }//--- вручную игнорируем заграничные значения
                            }
                        }
                    }
                }
                inRs.close()

                //--- если данных вообще больше нет или нашлись все последние требуемые данные, то выходим
                if( !isDataExist ) break
                if( ( oc.scg == null || result.pixPoint != null && result.objectAngle != null ) &&
                    ( hmSCS == null || hmSCS.size == result.tmSignalState.size ) &&
                    ( hmSCW == null || hmSCW.size == result.tmWorkState.size ) &&
                    ( hmSCLL == null || hmSCLL.size == result.tmLiquidLevel.size ) )

                    break

                //--- иначе продолжаем со следующей увеличенной порцией LIMIT
                lastLimit *= 2
            }

            return result
        }
    }
}

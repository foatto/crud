package foatto.mms.core_mms.calc

import foatto.core.app.UP_TIME_OFFSET
import foatto.core.app.graphic.*
import foatto.core.app.xy.XyProjection
import foatto.core.app.xy.geom.XyPoint
import foatto.sql.CoreAdvancedStatement
import foatto.core.util.*
import foatto.core_server.app.server.UserConfig
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.ZoneData
import foatto.mms.core_mms.ZoneLimitData
import foatto.mms.core_mms.graphic.server.graphic_handler.LiquidGraphicHandler
import foatto.mms.core_mms.graphic.server.graphic_handler.iGraphicHandler
import foatto.mms.core_mms.sensor.SensorConfig
import foatto.mms.core_mms.sensor.SensorConfigA
import foatto.mms.core_mms.sensor.SensorConfigU
import foatto.mms.core_mms.sensor.SensorConfigW
import java.nio.ByteOrder
import java.time.ZoneId
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class ObjectCalc {

    lateinit var objectConfig: ObjectConfig

    //    public long lastDataTime = 0;

    var gcd: GeoCalcData? = null
    var tmWorkCalc = TreeMap<String, WorkCalcData>()
    var tmLiquidLevelCalc = TreeMap<String, LiquidLevelCalcData>()
    var tmLiquidLevelGroupSum = TreeMap<String, LiquidLevelCalcData>()
    var tmLiquidUsingCalc = TreeMap<String, LiquidUsingCalcData>()
    var tmEnergoCalc = TreeMap<String, Int>()
    var tmEnergoGroupSum = TreeMap<String, Int>()

    var sbGeoName = StringBuilder()
    var sbGeoRun = StringBuilder()
    var sbGeoOutTime = StringBuilder()
    var sbGeoInTime = StringBuilder()
    var sbGeoWayTime = StringBuilder()
    var sbGeoMovingTime = StringBuilder()
    var sbGeoParkingTime = StringBuilder()
    var sbGeoParkingCount = StringBuilder()

    var sbWorkName = StringBuilder()
    var sbWorkTotal = StringBuilder()
    var sbWorkMoving = StringBuilder()
    var sbWorkParking = StringBuilder()

    var sbLiquidLevelName = StringBuilder()
    var sbLiquidLevelBeg = StringBuilder()
    var sbLiquidLevelEnd = StringBuilder()
    var sbLiquidLevelIncTotal = StringBuilder()
    var sbLiquidLevelDecTotal = StringBuilder()
    var sbLiquidLevelUsingTotal = StringBuilder()
    var sbLiquidLevelUsingMoving = StringBuilder()
    var sbLiquidLevelUsingParking = StringBuilder()
    var sbLiquidLevelUsingCalc = StringBuilder()

    var sbLiquidUsingName = StringBuilder()
    var sbLiquidUsingTotal = StringBuilder()
    var sbLiquidUsingMoving = StringBuilder()
    var sbLiquidUsingParking = StringBuilder()

    var sbEnergoName = StringBuilder()
    var sbEnergoValue = StringBuilder()

    companion object {

        //--- максимально допустимый пробег между точками
        private val MAX_RUN = 100000 // 100 км. = полчаса ( см. MAX_WORK_TIME_INTERVAL ) при 200 км/ч

        //--- максимально допустимый интервал времени между точками, свыше которого:
        //--- 1. для гео-датчиков - за этот период пробег не считается
        //--- 2. для датчиков работы оборудования - этот период считается нерабочим, независимо от текущего состояния точки
        //--- 3. для датчиков уровня топлива - этот период считается нерабочим ( не расходом, не заправкой, не сливом ) и изменение уровня ни в какие суммы не входит
        const val MAX_WORK_TIME_INTERVAL = 30 * 60

        //--- максимальная продолжительность предыдущего "нормального" периода,
        //--- используемого для расчёта среднего расхода топлива во время заправки/слива
        private val MAX_CALC_PREV_NORMAL_PERIOD = 3 * 60 * 60

        fun calcObject(stm: CoreAdvancedStatement, userConfig: UserConfig, oc: ObjectConfig, begTime: Int, endTime: Int ): ObjectCalc {

            val result = ObjectCalc()
            result.objectConfig = oc

            val zoneId = getZoneId(userConfig.getUserProperty(UP_TIME_OFFSET)?.toIntOrNull())
            //--- единоразово загрузим данные по всем датчикам объекта
            val ( alRawTime, alRawData ) = loadAllSensorData( stm, oc, begTime, endTime )

            //--- последнее время точки с данными
            //if(  ! alRawTime.isEmpty()  ) result.lastDataTime = alRawTime.get(  alRawTime.size() - 1  );

            //--- если прописаны гео-датчики - суммируем метры пробега
            if( oc.scg != null ) {
                //--- в обычных расчётах нам не нужны точки траектории, поэтому даем максимальный масштаб.
                //--- превышения тоже не нужны, поэтому даём maxEnabledOverSpeed = 0
                result.gcd = calcGeoSensor( alRawTime, alRawData, oc, begTime, endTime, 1000000000, 0, null )

                //--- если задан норматив по жидкости( топливу ), посчитаем его
                if( oc.scg!!.isUseRun && /*oc.scg!!.liquidName != null &&*/ !oc.scg!!.liquidName.isEmpty() ) {
                    var lucd: LiquidUsingCalcData? = result.tmLiquidUsingCalc[ oc.scg!!.liquidName ]
                    if( lucd == null ) {
                        lucd = LiquidUsingCalcData()
                        result.tmLiquidUsingCalc[ oc.scg!!.liquidName ] = lucd
                    }
                    lucd.add( oc.scg!!.liquidNorm * result.gcd!!.run / 100.0, oc.scg!!.liquidNorm * result.gcd!!.run / 100.0, 0.0, 0.0 )
                }
            }

            //--- датчики работы оборудования
            val hmSCW = oc.hmSensorConfig[ SensorConfig.SENSOR_WORK ]
            if( !hmSCW.isNullOrEmpty() ) {
                //--- для удобства выведем в отдельную переменную
                val alMovingAndParking = if( oc.scg != null && oc.scg!!.isUseSpeed ) result.gcd!!.alMovingAndParking else null
                for( portNum in hmSCW.keys ) {
                    val scw = hmSCW[ portNum ] as SensorConfigW

                    val wcd = calcWorkSensor( alRawTime, alRawData, oc, scw, begTime, endTime, alMovingAndParking )
                    result.tmWorkCalc[ scw.descr ] = wcd

                    //--- если задан норматив по жидкости( топливу ), посчитаем его
                    if( /*scw.liquidName != null &&*/ !scw.liquidName.isEmpty() ) {
                        var lucd: LiquidUsingCalcData? = result.tmLiquidUsingCalc[ scw.liquidName ]
                        if( lucd == null ) {
                            lucd = LiquidUsingCalcData()
                            result.tmLiquidUsingCalc[ scw.liquidName ] = lucd
                        }
                        lucd.add( scw.liquidNorm * wcd.onTime / 60.0 / 60.0,
                                  if( alMovingAndParking == null || alMovingAndParking.isEmpty() ) 0.0 else scw.liquidNorm * wcd.onMovingTime / 60.0 / 60.0,
                                  if( alMovingAndParking == null || alMovingAndParking.isEmpty() ) 0.0 else scw.liquidNorm * wcd.onParkingTime / 60.0 / 60.0, 0.0 )
                    }
                }
            }

            //--- датчики уровня жидкости
            val hmSCLL = oc.hmSensorConfig[ SensorConfig.SENSOR_LIQUID_LEVEL ]
            if( !hmSCLL.isNullOrEmpty() ) {
                //--- для удобства выведем в отдельную переменную
                val alMovingAndParking = if( oc.scg != null && oc.scg!!.isUseSpeed ) result.gcd!!.alMovingAndParking else null
                for( portNum in hmSCLL.keys ) {
                    val sca = hmSCLL[ portNum ] as SensorConfigA

                    val llcd = calcLiquidLevelSensor( alRawTime, alRawData, oc, sca, begTime, endTime, result.gcd, result.tmWorkCalc, result.tmEnergoCalc, stm )
                    result.tmLiquidLevelCalc[ sca.descr ] = llcd

                    //--- посчитаем групповую сумму
                    var llcdGroupSum: LiquidLevelCalcData? = result.tmLiquidLevelGroupSum[ sca.sumGroup ]
                    if( llcdGroupSum == null ) {
                        llcdGroupSum = LiquidLevelCalcData( sca.sumGroup )
                        result.tmLiquidLevelGroupSum[ sca.sumGroup ] = llcdGroupSum
                    }
                    llcdGroupSum.begLevel += llcd.begLevel
                    llcdGroupSum.endLevel += llcd.endLevel
                    llcdGroupSum.incTotal += llcd.incTotal
                    llcdGroupSum.decTotal += llcd.decTotal
                    llcdGroupSum.usingTotal += llcd.usingTotal
                    llcdGroupSum.usingMoving += llcd.usingMoving
                    llcdGroupSum.usingParking += llcd.usingParking
                    if( llcd.usingCalc != null ) llcdGroupSum.usingCalc = ( if( llcdGroupSum.usingCalc == null ) 0.0 else llcdGroupSum.usingCalc!! ) + llcd.usingCalc!!

                    //--- если задано наименование жидкости ( топлива ), то добавим его расход в общий список
                    if( /*sca.liquidName != null &&*/ !sca.liquidName.isEmpty() ) {
                        //--- для измеренных уровней
                        val liquidName = sca.liquidName
                        var lucd: LiquidUsingCalcData? = result.tmLiquidUsingCalc[ liquidName ]
                        if( lucd == null ) {
                            lucd = LiquidUsingCalcData()
                            result.tmLiquidUsingCalc[ liquidName ] = lucd
                        }
                        lucd.add( llcd.usingTotal, if( alMovingAndParking == null || alMovingAndParking.isEmpty() ) 0.0 else llcd.usingMoving,
                                  if( alMovingAndParking == null || alMovingAndParking.isEmpty() ) 0.0 else llcd.usingParking,
                                  if( llcd.usingCalc == null ) 0.0 else llcd.usingCalc!! )
                    }
                }
            }

            //--- датчики объёмного расхода жидкости ( расходомеры ) - не лучше ли считать SENSOR_MASS_FLOW?
            val hmSCVF = oc.hmSensorConfig[ SensorConfig.SENSOR_VOLUME_FLOW ]
            if( !hmSCVF.isNullOrEmpty() ) {
                for( portNum in hmSCVF.keys ) {
                    val scu = hmSCVF[ portNum ] as SensorConfigU
                    //--- если задано наименование жидкости ( топлива ), то добавим его расход в общий список
                    if( scu.liquidName.isNotEmpty() ) {
                        var lucd: LiquidUsingCalcData? = result.tmLiquidUsingCalc[ scu.liquidName ]
                        if( lucd == null ) {
                            lucd = LiquidUsingCalcData()
                            result.tmLiquidUsingCalc[ scu.liquidName ] = lucd
                        }
                        calcLiquidUsingSensor( alRawTime, alRawData, oc, scu, begTime, endTime,
                                               if( oc.scg != null && oc.scg!!.isUseSpeed ) result.gcd!!.alMovingAndParking else null, lucd )
                    }
                }
            }

            //--- датчики - электросчётчики
            val hmSCE = oc.hmSensorConfig[ SensorConfig.SENSOR_ENERGO_COUNT_AD ]
            if( !hmSCE.isNullOrEmpty() ) {
                for( sce in hmSCE.values ) {
                    val e = calcEnergoSensor( alRawTime, alRawData, oc, sce, begTime, endTime )
                    result.tmEnergoCalc[ sce.descr ] = e

                    //--- посчитаем групповую сумму
                    val eGroupSum = result.tmEnergoGroupSum[ sce.sumGroup ]
                    result.tmEnergoGroupSum[ sce.sumGroup ] = ( eGroupSum ?: 0 ) + e
                }
            }

            //--- заполнение типовых строк вывода ( для табличных форм и отчетов )
            if( oc.scg != null )
                fillGeoString( result.gcd!!, zoneId, result.sbGeoName, result.sbGeoRun, result.sbGeoOutTime, result.sbGeoInTime, result.sbGeoWayTime,
                               result.sbGeoMovingTime, result.sbGeoParkingTime, result.sbGeoParkingCount )
            fillWorkString( result.tmWorkCalc, result.sbWorkName, result.sbWorkTotal, result.sbWorkMoving, result.sbWorkParking )
            fillLiquidLevelString( result.tmLiquidLevelCalc, result.sbLiquidLevelName, result.sbLiquidLevelBeg, result.sbLiquidLevelEnd, result.sbLiquidLevelIncTotal,
                    result.sbLiquidLevelDecTotal, result.sbLiquidLevelUsingTotal, result.sbLiquidLevelUsingMoving, result.sbLiquidLevelUsingParking, result.sbLiquidLevelUsingCalc )
            fillLiquidUsingString( result.tmLiquidUsingCalc, result.sbLiquidUsingName, result.sbLiquidUsingTotal, result.sbLiquidUsingMoving, result.sbLiquidUsingParking )
            fillEnergoString( result.tmEnergoCalc, result.sbEnergoName, result.sbEnergoValue )

            return result
        }

        fun loadAllSensorData(stm: CoreAdvancedStatement, oc: ObjectConfig, begTime: Int, endTime: Int ): Pair<List<Int>,List<AdvancedByteBuffer>> {

            //--- определим максимальное сглаживание
            var maxSmoothTime = 0
            for( sensorType in oc.hmSensorConfig.keys ) {
                //--- в датчиках работы оборудования, расхода жидкости и электроэнергии нет параметра сглаживания
                if( SensorConfig.hsSensorNonSmooth.contains( sensorType ) ) continue

                val hmSC = oc.hmSensorConfig[ sensorType ]!!
                for( portNum in hmSC.keys ) {
                    val sca = hmSC[ portNum ] as SensorConfigA
                    maxSmoothTime = max( maxSmoothTime, sca.smoothTime )
                }
            }

            //--- соберём сырые необработанные данные
            val alRawTime = mutableListOf<Int>()
            val alRawData = mutableListOf<AdvancedByteBuffer>()

            //--- чтобы не пропадали начальные/конечные точки в периодах
            //--- и чтобы совпадали конечные уровни одного периода и начальные уровни следующего -
            //--- берём диапазоны с запасом для сглаживания
            val sql =
                " SELECT ontime , sensor_data FROM MMS_data_${oc.objectID} " +
                " WHERE ontime >= ${begTime - maxSmoothTime} AND ontime <= ${endTime + maxSmoothTime} " +
                " ORDER BY ontime "

            val inRs = stm.executeQuery( sql )
            while( inRs.next() ) {
                alRawTime.add( inRs.getInt( 1 ) )
                alRawData.add( inRs.getByteBuffer( 2, ByteOrder.BIG_ENDIAN ) )
            }
            inRs.close()

            return Pair( alRawTime, alRawData )
        }

        fun calcGeoSensor( alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, oc: ObjectConfig, begTime: Int, endTime: Int, scale: Int, maxEnabledOverSpeed: Int,
                           alZoneSpeedLimit: List<ZoneLimitData>? ): GeoCalcData {
            val scg = oc.scg

            var begRun = 0
            var lastRun = 0
            var run = 0

            var movingBeginTime = 0           // время начала движения
            var parkingBeginTime = 0          // время начала остановки
            var parkingCoord: XyPoint? = null        // координаты стоянки

            val alMovingAndParking = mutableListOf<AbstractPeriodData>()
            val alOverSpeed = mutableListOf<AbstractPeriodData>()
            val alPointTime = mutableListOf<Int>()
            val alPointXY = mutableListOf<XyPoint>()
            val alPointSpeed = mutableListOf<Int>()
            val alPointOverSpeed = mutableListOf<Int>()

            var normalSpeedBeginTime = 0      // время начала нормальной скорости
            var overSpeedBeginTime = 0
            var maxOverSpeedTime = 0          // время максимального превышения
            var maxOverSpeedCoord: XyPoint? = null   // координаты точки максимального превышения
            var maxOverSpeedMax = 0            // скорость при максимальном превышении
            var maxOverSpeedDiff = 0           // величина максимального превышения

            //--- для генерализации
            val lastPoint = XyPoint(0, 0)

            var lastTime = 0
            var curTime = 0
            for( i in alRawTime.indices ) {
                curTime = alRawTime[ i ]
                //--- данные до запрашиваемого диапазона ( расширенные для сглаживания )
                //--- в данном случае не интересны и их можно пропустить
                if( curTime < begTime ) continue
                //--- данные после запрашиваемого диапазона ( расширенные для сглаживания )
                //--- в данном случае не интересны и можно прекращать обработку
                if( curTime > endTime ) {
                    //--- точка за краем диапазона нам не нужна, возвращаем предыдущую точку
                    if( i > 0 ) curTime = alRawTime[ i - 1 ]
                    break
                }

                val gd = AbstractObjectStateCalc.getGeoData( oc, alRawData[ i ] ) ?: continue
                //--- самих геоданных может и не оказаться
                //            boolean curValue = (  lastTime == 0 || curTime - lastTime <= MAX_WORK_TIME_INTERVAL  ) &&
                //                               sensorData >= scw.minIgnore && sensorData <= scw.maxIgnore &&
                //                               (  scw.activeValue == 0  ) ^ (  sensorData > scw.boundValue  );

                //--- Нюансы расчёта пробега:
                //--- 1. Независимо от способа выдачи пробега ( относительный/межточечный или абсолютный ):
                //--- 1.1. Игнорируем пробеги между точками со слишком долгим промежутком по времени.
                //--- 1.2. Игнорируем точки со слишком большим/нереальным межточечным пробегом.
                //--- 2. Приборы, дающие в своих показаниях абсолютный пробег, умудряются его сбрасывать посреди дня,
                //---    приходится ловить каждый такой сброс ( алгоритм в чем-то схож с поиском заправок/сливов ),
                //---    но пропуская внезапные точки с нулевым пробегом при абсолютно нормальных координатах.
                if( gd.distance > 0 ) {
                    if( scg!!.isAbsoluteRun ) {
                        val curRun = gd.distance

                        //--- первоначальная инициализация
                        if( begRun == 0 ) begRun = curRun
                        else if( curRun < lastRun || curRun - lastRun > MAX_RUN || lastTime != 0 && curTime - lastTime > MAX_WORK_TIME_INTERVAL ) {
                            //--- суммируем подпробег
                            run += lastRun - begRun
                            //--- начинаем новый диапазон
                            begRun = curRun
                        }//--- точка сброса пробега
                        lastRun = curRun
                    }
                    else if( gd.distance < MAX_RUN ) run += gd.distance
                }

                val pixPoint = XyProjection.wgs_pix( gd.wgs )

                var overSpeed = 0

                //--- на стоянке ?
                if( gd.speed <= AbstractObjectStateCalc.MAX_SPEED_AS_PARKING ) {
                    //--- запись предыдущего движения
                    if( movingBeginTime != 0 ) {
                        alMovingAndParking.add( GeoPeriodData( movingBeginTime, curTime, 1 ) )
                        movingBeginTime = 0
                    }
                    //--- начало стоянки ( parkingBeginTime - флаг стоянки )
                    if( parkingBeginTime == 0 ) {
                        parkingBeginTime = curTime
                        parkingCoord = pixPoint
                    }
                    //--- стоянка = конец превышения и как бы начало нормальной скорости
                    if( overSpeedBeginTime != 0 ) {
                        alOverSpeed.add( OverSpeedPeriodData( overSpeedBeginTime, curTime, maxOverSpeedTime, maxOverSpeedCoord!!, maxOverSpeedMax, maxOverSpeedDiff ) )
                        overSpeedBeginTime = 0
                        maxOverSpeedTime = 0
                        maxOverSpeedCoord = null
                        maxOverSpeedMax = 0
                        maxOverSpeedDiff = 0
                    }
                    if( normalSpeedBeginTime == 0 ) normalSpeedBeginTime = curTime
                }
                else {
                    //--- запись предыдущей стоянки
                    if( parkingBeginTime != 0 ) {
                        alMovingAndParking.add( GeoPeriodData( parkingBeginTime, curTime, parkingCoord!! ) )
                        parkingBeginTime = 0
                        parkingCoord = null
                    }
                    //--- начало движения
                    if( movingBeginTime == 0 ) movingBeginTime = curTime

                    //--- обработка превышений
                    overSpeed = calcOverSpeed( scg!!.maxSpeedLimit, alZoneSpeedLimit, pixPoint, gd.speed )
                    if( overSpeed > maxEnabledOverSpeed ) {
                        //--- запишем предыдущее нормальное движение
                        if( normalSpeedBeginTime != 0 ) {
                            alOverSpeed.add( OverSpeedPeriodData( normalSpeedBeginTime, curTime ) )
                            normalSpeedBeginTime = 0
                        }
                        //--- отметка начала превышения
                        if( overSpeedBeginTime == 0 ) overSpeedBeginTime = curTime
                        //--- сохранение величины/координат/времени максимального превышения на участке
                        if( overSpeed > maxOverSpeedDiff ) {
                            maxOverSpeedTime = curTime
                            maxOverSpeedCoord = pixPoint
                            maxOverSpeedMax = gd.speed
                            maxOverSpeedDiff = overSpeed
                        }
                    }
                    else {
                        if( overSpeedBeginTime != 0 ) {
                            alOverSpeed.add( OverSpeedPeriodData( overSpeedBeginTime, curTime, maxOverSpeedTime, maxOverSpeedCoord!!, maxOverSpeedMax, maxOverSpeedDiff ) )
                            overSpeedBeginTime = 0
                            maxOverSpeedTime = 0
                            maxOverSpeedCoord = null
                            maxOverSpeedMax = 0
                            maxOverSpeedDiff = 0
                        }
                        if( normalSpeedBeginTime == 0 ) normalSpeedBeginTime = curTime
                    }
                }
                //--- для траектории записываются только точки с движением
                if( gd.speed > AbstractObjectStateCalc.MAX_SPEED_AS_PARKING && pixPoint.distance( lastPoint ) > scale ) {
                    alPointTime.add( curTime )
                    alPointXY.add( pixPoint )
                    alPointSpeed.add( gd.speed )
                    alPointOverSpeed.add( if( overSpeed < 0 ) 0 else overSpeed )

                    lastPoint.set( pixPoint )
                }

                lastTime = curTime
            }
            //--- суммируем подпробег последнего ( т.е. неоконченного ) диапазона
            if( scg!!.isAbsoluteRun ) run += lastRun - begRun
            //--- запись последнего незакрытого события
            if( movingBeginTime != 0 ) alMovingAndParking.add( GeoPeriodData( movingBeginTime, curTime, 1 ) )
            if( parkingBeginTime != 0 ) alMovingAndParking.add( GeoPeriodData( parkingBeginTime, curTime, parkingCoord!! ) )
            if( normalSpeedBeginTime != 0 ) alOverSpeed.add( OverSpeedPeriodData( normalSpeedBeginTime, curTime ) )
            if( overSpeedBeginTime != 0 ) alOverSpeed.add( OverSpeedPeriodData( overSpeedBeginTime, curTime, maxOverSpeedTime, maxOverSpeedCoord!!, maxOverSpeedMax, maxOverSpeedDiff ) )

            mergePeriods( alMovingAndParking, scg.minMovingTime, scg.minParkingTime )
            mergePeriods( alOverSpeed, scg.minOverSpeedTime, max( 10, scg.minOverSpeedTime / 10 ) )

            //--- подсчёт прочих показателей: время выезда/заезда, в движении/на стоянке, кол-во стоянок
            var outTime = 0
            var inTime = 0
            var movingTime = 0
            var parkingCount = 0
            var parkingTime = 0
            for( i in alMovingAndParking.indices ) {
                val pd = alMovingAndParking[ i ] as GeoPeriodData

                if( pd.getState() != 0 ) {
                    if( outTime == 0 ) outTime = pd.begTime
                    inTime = pd.endTime
                    movingTime += pd.endTime - pd.begTime
                }
                else {
                    parkingCount++
                    parkingTime += pd.endTime - pd.begTime
                }
            }

            //--- переводим суммы метров в км
            //--- ( если пробег от этого датчика не используется, то сделаем его отрицательным )
            return GeoCalcData( scg.descr, run / 1000.0 * scg.runKoef, outTime, inTime, movingTime, parkingCount, parkingTime,
                                alMovingAndParking, alOverSpeed, alPointTime, alPointXY, alPointSpeed, alPointOverSpeed )
        }

        fun calcWorkSensor( alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, oc: ObjectConfig, scw: SensorConfigW, begTime: Int, endTime: Int,
                            alMovingAndParking: List<AbstractPeriodData>? ): WorkCalcData {

            val alResult = mutableListOf<AbstractPeriodData>()

            var workBeginTime = 0       // время начала работы
            var delayBeginTime = 0      // время начала простоя

            var lastTime = 0
            var curTime = 0
            for( i in alRawTime.indices ) {
                curTime = alRawTime[ i ]
                //--- данные до запрашиваемого диапазона ( расширенные для сглаживания )
                //--- в данном случае не интересны и их можно пропустить
                if( curTime < begTime ) continue
                //--- данные после запрашиваемого диапазона ( расширенные для сглаживания )
                //--- в данном случае не интересны и можно прекращать обработку
                if( curTime > endTime ) {
                    //--- точка за краем диапазона нам не нужна, возвращаем предыдущую точку
                    if( i > 0 ) curTime = alRawTime[ i - 1 ]
                    break
                }

                //--- преобразование данных --------------------------------------------------------------------

                val sensorData = AbstractObjectStateCalc.getSensorData( oc, scw.portNum, alRawData[ i ] )?.toInt() ?: 0
                //--- вручную игнорируем:
                //--- 1. слишком длинные интервалы между точками ( кроме первой точки )
                //--- 2. заграничные значения
                //--- ( новое условие - не академически/бесполезно ИГНОРИРУЕМ, а считаем, что оборудование вне заданных границ НЕ РАБОТАЕТ )
                //if(  sensorData < scw.minIgnore || sensorData > scw.maxIgnore  ) continue;
                var curValue =
                    ( lastTime == 0 || curTime - lastTime <= MAX_WORK_TIME_INTERVAL ) &&
                    sensorData >= scw.minIgnore &&
                    sensorData <= scw.maxIgnore &&
                    (( scw.activeValue == 0 ) xor ( sensorData > scw.boundValue ))

                //--- учёт модификатора работы датчика - учёт работы только в движении или только на стоянке
                if( curValue && alMovingAndParking != null && !alMovingAndParking.isEmpty() ) {
                    if( scw.calcInMoving xor scw.calcInParking ) {
                        //--- ищем период, в который попадает данная точка
                        //--- ( поскольку конец периода всегда == началу следующего периода,
                        //--- а нам важнее начало нового периода, нежели конец предыдущего - то ищем с конца списка )
                        for( j in alMovingAndParking.indices.reversed() ) {
                            val mpd = alMovingAndParking[ j ] as GeoPeriodData
                            if( curTime >= mpd.begTime && curTime <= mpd.endTime ) {
                                curValue = if( mpd.moveState != 0 ) scw.calcInMoving else scw.calcInParking
                                break
                            }
                        }
                    }
                    else curValue = curValue and ( scw.calcInMoving && scw.calcInParking )//--- если обе галочки выключены - датчик никогда не считается
                }
                //--- таки всё ещё в работе?
                if( curValue ) {
                    //--- запись предыдущего простоя
                    if( delayBeginTime != 0 ) {
                        alResult.add( WorkPeriodData( delayBeginTime, curTime, 0 ) )
                        delayBeginTime = 0
                    }
                    //--- начало работы
                    if( workBeginTime == 0 ) workBeginTime = curTime
                }
                else {
                    //--- запись предыдущей работы
                    if( workBeginTime != 0 ) {
                        alResult.add( WorkPeriodData( workBeginTime, curTime, 1 ) )
                        workBeginTime = 0
                    }
                    //--- начало простоя
                    if( delayBeginTime == 0 ) delayBeginTime = curTime
                }
                lastTime = curTime
            }

            //--- запись последнего незакрытого события
            if( workBeginTime != 0 ) alResult.add( WorkPeriodData( workBeginTime, curTime, 1 ) )
            if( delayBeginTime != 0 ) alResult.add( WorkPeriodData( delayBeginTime, curTime, 0 ) )

            //--- слияние периодов работы/простоя в соответствии с минимальными продолжительностями
            mergePeriods( alResult, scw.minOnTime, scw.minOffTime )

            //--- при наличии гео-датчика посчитаем распределение работы/простоя
            //--- по заданным периодам движения/стоянок
            return WorkCalcData( alResult, if( alMovingAndParking == null ) null
                                           else if( alMovingAndParking.isEmpty() ) mutableListOf()
                                           else multiplePeriods( alMovingAndParking, alResult ) )
        }

        fun calcLiquidLevelSensor( alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, oc: ObjectConfig, sca: SensorConfigA, begTime: Int, endTime: Int,
                                   gcd: GeoCalcData?, tmWorkCalc: TreeMap<String, WorkCalcData>, tmEnergoCalc: TreeMap<String, Int>,
                                   stm: CoreAdvancedStatement
        ): LiquidLevelCalcData {

            val aLine = GraphicDataContainer( GraphicDataContainer.ElementType.LINE, 0, 2 )
            val alLSPD = mutableListOf<LiquidStatePeriodData>()
            getSmoothLiquidGraphicData( alRawTime, alRawData, oc, sca, begTime, endTime, aLine, alLSPD )

            val llcd = LiquidLevelCalcData( aLine, alLSPD )
            calcLiquidUsingByLevel( sca, llcd, if( oc.scg != null && oc.scg!!.isUseSpeed ) gcd!!.alMovingAndParking else null, stm, oc, begTime, endTime )

            llcd.sumGroup = sca.sumGroup

            //--- сразу добавим ссылок на работу гео-датчиков, оборудования и электросчётчиков в той же группе, что и уровнемер

            if( oc.scg != null && oc.scg!!.group == sca.group ) llcd.gcd = gcd

            val hmSCW = oc.hmSensorConfig[ SensorConfig.SENSOR_WORK ]
            hmSCW?.values?.forEach {
                if( it.group == sca.group && tmWorkCalc[ it.descr ] != null )
                    llcd.tmWorkCalc[ it.descr ] = tmWorkCalc[ it.descr ]!!
            }

            val hmSCE = oc.hmSensorConfig[ SensorConfig.SENSOR_ENERGO_COUNT_AD ]
            hmSCE?.values?.forEach {
                if( it.group == sca.group && tmEnergoCalc[ it.descr ] != null )
                    llcd.tmEnergoCalc[ it.descr ] = tmEnergoCalc[ it.descr ]!!
            }

            return llcd
        }

        //--- определение расхода жидкости по расходомеру
        fun calcLiquidUsingSensor( alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, oc: ObjectConfig, scu: SensorConfigU, begTime: Int, endTime: Int,
                                   alMovingAndParking: List<AbstractPeriodData>?, lucd: LiquidUsingCalcData ) {
            var usingMoving = 0.0
            var usingParking = 0.0
            if( alMovingAndParking != null ) {
                for( apd in alMovingAndParking ) {
                    val usingForPeriod = getSensorCountData( alRawTime, alRawData, oc, scu, apd.begTime, apd.endTime )
                    //--- в движении
                    if( apd.getState() != 0 ) usingMoving += usingForPeriod
                    else usingParking += usingForPeriod
                }
            }
            //--- принципиально считаем общий расход отдельно от "в движении/на стоянке",
            //--- чтобы сразу выявить возможные ошибки в расчётах
            lucd.add( getSensorCountData( alRawTime, alRawData, oc, scu, begTime, endTime ), usingMoving, usingParking, 0.0 )
        }

        //--- расчёт электроэнергии
        fun calcEnergoSensor( alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, oc: ObjectConfig, sce: SensorConfig, begTime: Int, endTime: Int ): Int {
            //--- счётчики, дающие в своих показаниях абсолютные значения, могут его сбросить по команде или переполнении.
            //--- придётся ловить каждый такой сброс ( алгоритм в чем-то схож с поиском заправок/сливов )
            //--- также пропускаем внезапные точки с нулевым счётчиком
            var begE = 0
            var lastE = 0
            var energo = 0

            for( i in alRawTime.indices ) {
                val curTime = alRawTime[ i ]
                //--- данные до запрашиваемого диапазона ( расширенные для сглаживания )
                //--- в данном случае не интересны и их можно пропустить
                if( curTime < begTime ) continue
                //--- данные после запрашиваемого диапазона ( расширенные для сглаживания )
                //--- в данном случае не интересны и можно прекращать обработку
                if( curTime > endTime ) break

                //--- самих энергоданных может и не оказаться
                val energoCountActiveDirect = AbstractObjectStateCalc.getSensorData( oc, sce.portNum, alRawData[ i ] )?.toInt() ?: continue

                //--- электроэнергию считаем только для точек с ненулевым значением активной прямой э/энергии
                if( energoCountActiveDirect > 0 ) {
                    val curE = energoCountActiveDirect

                    //--- первоначальная инициализация
                    if( begE == 0 ) begE = curE
                    else if( curE < lastE ) {
                        //--- суммируем подзначение
                        energo += lastE - begE
                        //--- начинаем новый диапазон
                        begE = curE
                    }//--- точка сброса счётчика
                    lastE = curE
                    //}
                    //else run += gd.distance;
                }

            }
            //--- суммируем подзначение последнего ( т.е. неоконченного ) диапазона
            //if(  scg.isAbsoluteRun  )
            energo += lastE - begE

            return energo
        }

        //--------------------------------------------------------------------------------------------------------------

        //--- собираем величину максимального превышения для данной точки ( с учетом зон с ограничением по скорости )
        fun calcOverSpeed(maxSpeedConst: Int, alZoneSpeedLimit: List<ZoneLimitData>?, /*long pointTime, */ prjPoint: XyPoint, speed: Int ): Int {
            //--- собираем максимальное превышение
            var maxOverSpeed = -Integer.MAX_VALUE

            //--- ищем превышение среди постоянного ограничения
            maxOverSpeed = max( maxOverSpeed, speed - maxSpeedConst )
            //--- ищем среди зон с ограничением по скорости
            if( alZoneSpeedLimit != null )
                for( zd in alZoneSpeedLimit ) {
                    //--- если есть ограничения по времени действия, то проверим в их входимость,
                    //--- и если в интервал( ы ) времени действия зоны мы не входим, то переходим к следующей зоне
                    //if(  ! checkZoneInTime(  zd, pointTime  )  ) continue; - пока не применяется
                    //--- проверим геометрическую входимость в зону
                    if( !zd.zoneData!!.polygon!!.isContains( prjPoint ) ) continue
                    //--- проверим на наличие превышения
                    maxOverSpeed = max( maxOverSpeed, speed - zd.maxSpeed )
                }
            return maxOverSpeed
        }

        //--- пока не применяется
        //    private static boolean checkZoneInTime(  ZoneLimitData zd, long pointTime  ) throws Exception {
        //        //--- если ограничений нет, то все пучком : )
        //        if(  zd.alBeg.isEmpty()  ) return true;
        //        //--- если есть ограничения по времени действия, проверим в их входимость
        //        else {
        //            for(  int j = 0; j < zd.alBeg.size(); j++  )
        //                if(  pointTime >= zd.alBeg.get(  j  ) && pointTime <= zd.alEnd.get(  j  )  ) return true; // достаточно войти в один интервал
        //            return false;
        //        }
        //    }

        private fun mergePeriods( alPD: MutableList<AbstractPeriodData>, minOnTime: Int, minOffTime: Int ) {
            //--- выбрасывание недостаточно продолжительных периодов вкл/выкл
            //--- и последующее слияние соседних с ним выкл/вкл соответственно
            while( true ) {
                var isShortFound = false
                var i = 0
                while( i < alPD.size ) {
                    val pd = alPD[ i ]
                    //--- если период слишком короткий для учета
                    if( pd.endTime - pd.begTime < ( if( pd.getState() != 0 ) minOnTime else minOffTime ) ) {
                        //--- если рядом есть хотя бы один длинный противоположный период - то убираем короткий и соединяем соседние
                        var isLongFound = false
                        if( i > 0 ) {
                            val pdPrev = alPD[ i - 1 ]
                            isLongFound = isLongFound or ( pdPrev.endTime - pdPrev.begTime >= if( pdPrev.getState() != 0 ) minOnTime else minOffTime )
                        }
                        if( i < alPD.size - 1 ) {
                            val pdNext = alPD[ i + 1 ]
                            isLongFound = isLongFound or ( pdNext.endTime - pdNext.begTime >= if( pdNext.getState() != 0 ) minOnTime else minOffTime )
                        }
                        //--- найден( ы ) длинный( е ) сосед( и )
                        if( isLongFound ) {
                            //--- первый короткий период
                            if( i == 0 ) {
                                alPD[ 1 ].begTime = alPD[ 0 ].begTime
                                alPD.removeAt( 0 )
                                i = 1    // текущий период уже длинный, идем сразу дальше
                            }
                            else if( i == alPD.size - 1 ) {
                                alPD[ i - 1 ].endTime = alPD[ i ].endTime
                                alPD.removeAt( i )
                                i++    // текущий период уже длинный, идем сразу дальше ( хотя для последнего периода это уже не обязательно )
                            }
                            else {
                                alPD[ i - 1 ].endTime = alPD[ i + 1 ].endTime
                                alPD.removeAt( i )   // удаляем текущий короткий период
                                alPD.removeAt( i )   // удаляем следующий противоположный период, слитый с предыдущим противоположным
                                //i++ - делать не надо, т.к. текущий период теперь новый с неизвестной длительностью
                            }//--- серединный короткий период
                            //--- последний короткий период
                            isShortFound = true    // удаление было, есть смысл пройтись по цепочке еще раз
                        }
                        else i++ // ни одного длинного соседа не найдено - идем дальше
                    }
                    else i++
                }
                //--- больше нечего выбрасывать и соединять
                if( !isShortFound ) break
            }
        }

        private fun multiplePeriods( alPD1: List<AbstractPeriodData>, alPD2: List<AbstractPeriodData> ): List<MultiplePeriodData> {
            val alResult = mutableListOf<MultiplePeriodData>()

            var pos1 = 0
            var pos2 = 0
            while( pos1 < alPD1.size && pos2 < alPD2.size ) {
                val pd1 = alPD1[ pos1 ]
                val pd2 = alPD2[ pos2 ]

                //--- полное несовпадение: первый совсем раньше второго
                //--- 1: -|===|-----
                //--- 2: -----|===|-
                if( pd1.endTime <= pd2.begTime ) {
                    alResult.add( MultiplePeriodData( pd1.begTime, pd1.endTime, pd1.getState(), 0 ) )
                    pos1++
                }
                else if( pd2.endTime <= pd1.begTime ) {
                    alResult.add( MultiplePeriodData( pd2.begTime, pd2.endTime, 0, pd2.getState() ) )
                    pos2++
                }
                else if( pd1.begTime <= pd2.begTime ) {
                    //--- "пустое время" между началом первого и второго периодов возможно только,
                    //--- если второй период - начальный элемент в своем списке ( т.е. перед ним нет данных ),
                    //--- а иначе этот отрезок должен был быть записан на предыдущем шаге
                    //--- 1: -|=======|-   -|=====|-   -|===|---
                    //--- 2: -|?|===|---   -|?|===|-   -|?|===|-
                    if( pd1.begTime < pd2.begTime && pos2 == 0 ) alResult.add( MultiplePeriodData( pd1.begTime, pd2.begTime, pd1.getState(), 0 ) )
                    //--- подварианты: второй закончился раньше первого - т.е. полностью входит в первый
                    //--- или закончился одновременно с первым
                    //--- 1: -|=======|-   -|=====|-
                    //--- 2: ---|===|---   ---|===|-
                    //--- 1: -|=======|-   -|=====|-
                    //--- 2: -|=====|---   -|=====|-
                    if( pd2.endTime <= pd1.endTime ) {
                        alResult.add( MultiplePeriodData( pd2.begTime, pd2.endTime, pd1.getState(), pd2.getState() ) )
                        pos2++
                        if( pd1.endTime == pd2.endTime ) pos1++
                        else if( pos2 == alPD2.size ) {
                            alResult.add( MultiplePeriodData( pd2.endTime, pd1.endTime, pd1.getState(), 0 ) )
                            pos1++
                        }//--- если это был последний второй, то запишем остаток/хвост первого
                    }
                    //--- первый закончился раньше второго
                    //--- 1: -|===|---
                    //--- 2: ---|===|-
                    //--- 1: -|===|---
                    //--- 2: -|=====|-
                    else if( pd1.endTime < pd2.endTime ) {
                        alResult.add( MultiplePeriodData( pd2.begTime, pd1.endTime, pd1.getState(), pd2.getState() ) )
                        pos1++
                        //--- если это был последний первый, то запишем остаток/хвост второго
                        if( pos1 == alPD1.size ) {
                            alResult.add( MultiplePeriodData( pd1.endTime, pd2.endTime, 0, pd2.getState() ) )
                            pos2++
                        }
                    }
                }
                //--- частичное перекрытие: второй начался раньше первого
                //--- 1: ---|===|---   ---|===|-   ---|===|-
                //--- 2: -|=======|-   -|=====|-   -|===|---
                //--- частичное перекрытие: первый начался раньше второго или начались одинаково
                //--- 1: -|=======|-   -|=====|-   -|===|---
                //--- 2: ---|===|---   ---|===|-   ---|===|-
                //--- 1: -|=======|-   -|=====|-   -|===|---
                //--- 2: -|=====|---   -|=====|-   -|=====|-
                //--- полное несовпадение: второй совсем раньше первого
                //--- 1: -----|===|-
                //--- 2: -|===|-----
                else if( pd2.begTime < pd1.begTime ) {
                    //--- "пустое время" между началом второго и первого периодов возможно только,
                    //--- если первый период - начальный элемент в своем списке ( т.е. перед ним нет данных ),
                    //--- а иначе этот отрезок должен был быть записан на предыдущем шаге
                    //--- 1: -|?|===|---   -|?|===|-   -|?|===|-
                    //--- 2: -|=======|-   -|=====|-   -|===|---
                    if( pos1 == 0 ) alResult.add( MultiplePeriodData( pd2.begTime, pd1.begTime, 0, pd2.getState() ) )
                    //--- подварианты: первый закончился раньше второго - т.е. полностью входит во второй
                    //--- или закончился одновременно со вторым
                    //--- 1: ---|===|---   ---|===|-
                    //--- 2: -|=======|-   -|=====|-
                    if( pd1.endTime <= pd2.endTime ) {
                        alResult.add( MultiplePeriodData( pd1.begTime, pd1.endTime, pd1.getState(), pd2.getState() ) )
                        pos1++
                        if( pd1.endTime == pd2.endTime ) pos2++
                        else if( pos1 == alPD1.size ) {
                            alResult.add( MultiplePeriodData( pd1.endTime, pd2.endTime, 0, pd2.getState() ) )
                            pos2++
                        }//--- если это был последний первый, то запишем остаток/хвост второго
                    }
                    //--- второй закончился раньше первого
                    //--- 1: ---|===|-
                    //--- 2: -|===|---
                    else if( pd2.endTime < pd1.endTime ) {
                        alResult.add( MultiplePeriodData( pd1.begTime, pd2.endTime, pd1.getState(), pd2.getState() ) )
                        pos2++
                        //--- если это был последний второй, то запишем остаток/хвост первого
                        if( pos2 == alPD2.size ) {
                            alResult.add( MultiplePeriodData( pd2.endTime, pd1.endTime, pd1.getState(), 0 ) )
                            pos1++
                        }
                    }
                }
            }
            //--- дописать оставшиеся периоды у первой последовательности, если есть
            while( pos1 < alPD1.size ) {
                val pd1 = alPD1[ pos1 ]
                alResult.add( MultiplePeriodData( pd1.begTime, pd1.endTime, pd1.getState(), 0 ) )
                pos1++
            }
            //--- дописать оставшиеся периоды у второй последовательности, если есть
            while( pos2 < alPD2.size ) {
                val pd2 = alPD2[ pos2 ]
                alResult.add( MultiplePeriodData( pd2.begTime, pd2.endTime, 0, pd2.getState() ) )
                pos2++
            }
            return alResult
        }

        //--- сглаживание графика аналоговой величины
        fun getSmoothAnalogGraphicData( alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, oc: ObjectConfig, sca: SensorConfigA, begTime: Int, endTime: Int,
                                        xScale: Int, yScale: Double, aMinLimit: GraphicDataContainer?, aMaxLimit: GraphicDataContainer?,
                                        aPoint: GraphicDataContainer?, aLine: GraphicDataContainer?, gh: iGraphicHandler ) {

            val isStaticMinLimit = gh.isStaticMinLimit( sca )
            val isStaticMaxLimit = gh.isStaticMaxLimit( sca )
            val isDynamicMinLimit = gh.isDynamicMinLimit( sca )
            val isDynamicMaxLimit = gh.isDynamicMaxLimit( sca )

            //--- сразу добавить/установить статические ( постоянные ) ограничения, если они требуются/поддерживаются
            if( gh.isStaticMinLimit( sca ) ) gh.setStaticMinLimit( sca, begTime, endTime, aMinLimit )
            if( gh.isStaticMaxLimit( sca ) ) gh.setStaticMaxLimit( sca, begTime, endTime, aMaxLimit )

            //--- для сглаживания могут понадобиться данные до и после времени текущей точки,
            //--- поэтому заранее перегружаем/переводим данные
            val alSensorData = mutableListOf<Double?>()
            for( bb in alRawData ) alSensorData.add( gh.getRawData( oc, sca, bb ) )

            //--- переработка сырых данных -----------------------------------------------------------------------------------------

            for( pos in alRawTime.indices ) {
                val rawTime = alRawTime[ pos ]
                //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
                if( rawTime < begTime ) continue
                if( rawTime > endTime ) break

                //--- вставим первую и последнюю псевдоточки для бесшовной связки между периодами

                //--- значение датчика м.б. == null, например, игнорируемое ниже/выше заданных границ значений
                //--- ( фильтрация шума )
                val rawData = alSensorData[ pos ] ?: continue

                //--- добавление динамических границ/лимитов значений
                if( isDynamicMinLimit ) {
                    //--- если точка линии достаточно далека от предыдущей ( или просто первая : )
                    if( aMinLimit!!.alGLD.isEmpty() || rawTime - aMinLimit.alGLD[ aMinLimit.alGLD.size - 1 ].x > xScale )
                        gh.addDynamicMinLimit( rawTime, gh.getDynamicMinLimit( oc, sca, rawTime, rawData ), aMinLimit )
                }
                if( isDynamicMaxLimit ) {
                    //--- если точка линии достаточно далека от предыдущей ( или просто первая : )
                    if( aMaxLimit!!.alGLD.isEmpty() || rawTime - aMaxLimit.alGLD[ aMaxLimit.alGLD.size - 1 ].x > xScale )
                        gh.addDynamicMaxLimit( rawTime, gh.getDynamicMaxLimit( oc, sca, rawTime, rawData ), aMaxLimit )
                }

                //--- если включен показ точек
                if( aPoint != null ) {
                    val colorIndex = if( isStaticMinLimit && rawData < gh.getStaticMinLimit( sca ) ||
                                         isStaticMaxLimit && rawData > gh.getStaticMaxLimit( sca ) ||
                                         isDynamicMinLimit && rawData < gh.getDynamicMinLimit( oc, sca, rawTime, rawData ) ||
                                         isDynamicMaxLimit && rawData > gh.getDynamicMaxLimit( oc, sca, rawTime, rawData ) ) GraphicColorIndex.POINT_CRITICAL
                                     else GraphicColorIndex.POINT_NORMAL

                    val gpdLast = if( aPoint.alGPD.isEmpty() ) null else aPoint.alGPD[ aPoint.alGPD.size - 1 ]

                    if( gpdLast == null || rawTime - gpdLast.x > xScale || abs( rawData - gpdLast.y ) > yScale || colorIndex != gpdLast.colorIndex )
                        aPoint.alGPD.add( GraphicPointData(rawTime, rawData, colorIndex ) )
                }

                //--- если включен показ линий
                if( aLine != null ) {
                    //--- поиск левой границы диапазона сглаживания
                    var pos1 = pos - 1
                    while( pos1 >= 0 ) {
                        if( rawTime - alRawTime[ pos1 ] > sca.smoothTime ) break
                        pos1--
                    }
                    //--- поиск правой границы диапазона сглаживания
                    var pos2 = pos + 1
                    while( pos2 < alRawTime.size ) {
                        if( alRawTime[ pos2 ] - rawTime > sca.smoothTime ) break
                        pos2++
                    }

                    //--- собственно сглаживание
                    var sumValue: Double
                    var countValue: Int
                    val avgValue: Double
                    when( sca.smoothMethod ) {
                    //--- поскольку pos1 и pos2 находятся ЗА пределами диапазона сглаживания,
                    //--- то пропускаем их, начиная с pos1+1 и заканчивая ДО pos2

                        SensorConfigA.SMOOTH_METOD_MEDIAN            -> {
                            val alSubList = mutableListOf<Double>()
                            for( p in pos1 + 1 until pos2 ) {
                                val v = alSensorData[ p ] ?: continue
                                alSubList.add( v )
                            }
                            val arrValue = alSubList.toTypedArray()
                            Arrays.sort( arrValue )
                            //--- если кол-во значений нечётное, берём ровно середину
                            if( arrValue.size % 2 != 0 ) avgValue = arrValue[ arrValue.size / 2 ]
                            else {
                                val val1 = arrValue[ arrValue.size / 2 - 1 ]
                                val val2 = arrValue[ arrValue.size / 2 ]
                                avgValue = val1 + ( val2 - val1 ) / 2
                            }//--- иначе среднее арифметическое между двумя ближними к середине значениями
                        }

                        SensorConfigA.SMOOTH_METOD_AVERAGE           -> {
                            sumValue = 0.0
                            countValue = 0
                            for( p in pos1 + 1 until pos2 ) {
                                val v = alSensorData[ p ] ?: continue
                                sumValue += v
                                countValue++
                            }
                            avgValue = sumValue / countValue
                        }

                        SensorConfigA.SMOOTH_METOD_AVERAGE_SQUARE    -> {
                            sumValue = 0.0
                            countValue = 0
                            for( p in pos1 + 1 until pos2 ) {
                                val v = alSensorData[ p ] ?: continue
                                sumValue += v * v
                                countValue++
                            }
                            avgValue = sqrt( sumValue / countValue )
                        }

                        SensorConfigA.SMOOTH_METOD_AVERAGE_GEOMETRIC -> {
                            sumValue = 1.0   // будет умножение, поэтому начальное значение = 1
                            countValue = 0
                            for( p in pos1 + 1 until pos2 ) {
                                val v = alSensorData[ p ] ?: continue
                                sumValue *= v
                                countValue++
                            }
                            avgValue = sumValue.pow(1.0 / countValue)
                        }

                        else -> avgValue = 0.0
                    }

                    val gldLast = if( aLine.alGLD.isEmpty() ) null else aLine.alGLD[ aLine.alGLD.size - 1 ]

                    //--- если заданы граничные значения - смотрим по усреднённому avgValue,
                    //--- поэтому типовой getDynamicXXX из начала цикла нам не подходит
                    val prevTime = ( gldLast?.x ?: rawTime )
                    val prevData = gldLast?.y ?: avgValue
                    val curColorIndex = gh.getLineColorIndex( oc, sca, rawTime, avgValue, prevTime, prevData )

                    if( gldLast == null || rawTime - gldLast.x > xScale || abs( rawData - gldLast.y ) > yScale || curColorIndex != gldLast.colorIndex ) {

                        val gd = if( oc.scg == null ) null else AbstractObjectStateCalc.getGeoData( oc, alRawData[ pos ] )
                        aLine.alGLD.add( GraphicLineData(rawTime, avgValue, curColorIndex, if( gd == null ) null else XyProjection.wgs_pix( gd.wgs ) ) )
                    }
                }
            }
        }

        //--- собираем периоды состояний уровня жидкости ( заправка, слив, расход ) и примененяем фильтры по заправкам/сливам/расходу
        fun getLiquidStatePeriodData( sca: SensorConfigA, aLine: GraphicDataContainer, alLSPD: MutableList<LiquidStatePeriodData>, gh: LiquidGraphicHandler ) {
            //--- нулевой проход: собираем периоды из точек
            //--- начинаем с 1-й точки, т.к. 0-я точка всегда "нормальная"
            var begPos = 0
            var curColorIndex = gh.lineNormalColorIndex
            for( i in 1 until aLine.alGLD.size ) {
                val gdl = aLine.alGLD[ i ]
                val newColorIndex = gdl.colorIndex
                //--- начался период нового типа, оканчиваем предыдущий период другого типа
                if( newColorIndex != curColorIndex ) {
                    //--- предыдущий период закончился в предыдущей точке
                    val endPos = i - 1
                    //--- в периоде должно быть как минимум две точки, одноточечные периоды отбрасываем
                    //--- ( обычно это стартовая точка в "нормальном" состоянии )
                    if( begPos < endPos ) alLSPD.add( LiquidStatePeriodData( begPos, endPos, curColorIndex ) )
                    //--- новый период на самом деле начинается с предыдущей точки
                    begPos = i - 1
                    curColorIndex = newColorIndex
                }
            }
            //--- закончим последний период
            val endPos = aLine.alGLD.size - 1
            if( begPos < endPos ) alLSPD.add( LiquidStatePeriodData( begPos, endPos, curColorIndex ) )

            //--- первый проход: несущественные заправки/сливы превратим в "обычный" расход
            run {
                var pos = 0
                while( pos < alLSPD.size ) {
                    val lspd = alLSPD[ pos ]
                    //--- сразу же пропускаем пустые или нормальные периоды
                    if( lspd.colorIndex == gh.lineNoneColorIndex || lspd.colorIndex == gh.lineNormalColorIndex ) {
                        pos++
                        continue
                    }
                    //--- определим несущественность заправки/слива
                    val begGDL = aLine.alGLD[ lspd.begPos ]
                    val endGDL = aLine.alGLD[ lspd.endPos ]
                    var isFound = false
                    if( lspd.colorIndex == gh.lineCriticalColorIndex ) isFound = endGDL.y - begGDL.y < sca.detectIncMinDiff ||
                            //--- заодно ловим периоды с нулевой длиной
                            endGDL.x - begGDL.x < max( sca.detectIncMinLen, 1 )
                    else if( lspd.colorIndex == gh.lineWarningColorIndex ) isFound = -( endGDL.y - begGDL.y ) < sca.detectDecMinDiff ||
                            //--- заодно ловим периоды с нулевой длиной
                            endGDL.x - begGDL.x < max( sca.detectDecMinLen, 1 )
                    //--- найдена несущественная заправка/слив
                    if( isFound ) {
                        //--- ищем возможные нормальные периоды слева/справа для слияния
                        var prevNormalLSPD: LiquidStatePeriodData? = null
                        var nextNormalLSPD: LiquidStatePeriodData? = null
                        if( pos > 0 ) {
                            prevNormalLSPD = alLSPD[ pos - 1 ]
                            if( prevNormalLSPD.colorIndex != gh.lineNormalColorIndex ) prevNormalLSPD = null
                        }
                        if( pos < alLSPD.size - 1 ) {
                            nextNormalLSPD = alLSPD[ pos + 1 ]
                            if( nextNormalLSPD.colorIndex != gh.lineNormalColorIndex ) nextNormalLSPD = null
                        }
                        //--- оба соседних периода нормальные, все три сливаем в один
                        if( prevNormalLSPD != null && nextNormalLSPD != null ) {
                            prevNormalLSPD.endPos = nextNormalLSPD.endPos
                            alLSPD.removeAt( pos )
                            //--- это не опечатка и не ошибка: после удаления текущего периода следующий период
                            //--- становится текущим и тоже удаляется
                            alLSPD.removeAt( pos )
                            //--- после слияния трёх периодов pos уже указывает на следующую позицию,
                            //--- увеличивать счетчик не надо
                            //pos++;
                        }
                        else if( prevNormalLSPD != null ) {
                            prevNormalLSPD.endPos = lspd.endPos
                            alLSPD.removeAt( pos )
                            //--- после слияния двух периодов pos уже указывает на следующую позицию,
                            //--- увеличивать счетчик не надо
                            //pos++;
                        }
                        else if( nextNormalLSPD != null ) {
                            nextNormalLSPD.begPos = lspd.begPos
                            alLSPD.removeAt( pos )
                            pos++
                        }
                        else {
                            lspd.colorIndex = gh.lineNormalColorIndex
                            pos++
                        }//--- нет нормальных соседей, сами нормализуемся
                        //--- правый период нормальный, сливаемся с ним
                        //--- левый период нормальный, сливаемся с ним
                        //--- в любом случае, нормализуем "свои" точки сглаженного графика
                        for( i in lspd.begPos + 1..lspd.endPos ) aLine.alGLD[ i ].colorIndex = gh.lineNormalColorIndex
                    }
                    else pos++//--- иначе просто переходим к следущему периоду
                }
            }

            //--- второй проход - удлинняем заправки и сливы за счёт сокращения соседских нормальных периодов
            for( pos in alLSPD.indices ) {
                val lspd = alLSPD[ pos ]
                //--- сразу же пропускаем пустые или нормальные периоды
                if( lspd.colorIndex == gh.lineNoneColorIndex || lspd.colorIndex == gh.lineNormalColorIndex ) continue

                //--- ищем нормальный период слева, если надо
                val addTimeBefore = if( lspd.colorIndex == gh.lineCriticalColorIndex ) sca.incAddTimeBefore
                else sca.decAddTimeBefore
                if( addTimeBefore > 0 && pos > 0 ) {
                    val prevNormalLSPD = alLSPD[ pos - 1 ]
                    if( prevNormalLSPD.colorIndex == gh.lineNormalColorIndex ) {
                        val bt = aLine.alGLD[ lspd.begPos ].x
                        //--- удлинняем начало своего периода, укорачиваем предыдущий нормальный период с конца
                        //--- именно >, а не >=, чтобы не допустить одноточечных нормальных периодов ( begPos == endPos )
                        //--- после удлиннения текущего ненормального
                        var p = prevNormalLSPD.endPos - 1
                        while( p > prevNormalLSPD.begPos ) {
                            if( bt - aLine.alGLD[ p ].x > addTimeBefore ) break
                            p--
                        }
                        //--- допустимой является предыдущая позиция
                        p++
                        //--- есть куда удлинняться?
                        if( p < prevNormalLSPD.endPos ) {
                            prevNormalLSPD.endPos = p
                            lspd.begPos = p
                            //--- в любом случае, переотметим "свои" точки сглаженного графика
                            for( i in lspd.begPos + 1..lspd.endPos ) aLine.alGLD[ i ].colorIndex = lspd.colorIndex
                        }
                    }
                }
                //--- ищем нормальный период справа, если надо
                val addTimeAfter = if( lspd.colorIndex == gh.lineCriticalColorIndex ) sca.incAddTimeAfter
                else sca.decAddTimeAfter
                if( addTimeAfter > 0 && pos < alLSPD.size - 1 ) {
                    val nextNormalLSPD = alLSPD[ pos + 1 ]
                    if( nextNormalLSPD.colorIndex == gh.lineNormalColorIndex ) {
                        val et = aLine.alGLD[ lspd.endPos ].x
                        //--- удлинняем конец своего периода, укорачиваем следующий нормальный период с начала
                        //--- именно <, а не <=, чтобы не допустить одноточечных нормальных периодов ( begPos == endPos )
                        //--- после удлиннения текущего ненормального
                        var p = nextNormalLSPD.begPos + 1
                        while( p < nextNormalLSPD.endPos ) {
                            if( aLine.alGLD[ p ].x - et > addTimeAfter ) break
                            p++
                        }
                        //--- допустимой является предыдущая позиция
                        p--
                        //--- есть куда удлинняться?
                        if( p > nextNormalLSPD.begPos ) {
                            nextNormalLSPD.begPos = p
                            lspd.endPos = p
                            //--- в любом случае, переотметим "свои" точки сглаженного графика
                            for( i in lspd.begPos + 1..lspd.endPos ) aLine.alGLD[ i ].colorIndex = lspd.colorIndex
                        }
                    }
                }
            }

            //--- третий проход: удаляем несущественные ( короткие ) "нормальные" периоды между одинаковыми ненормальными
            var pos = 0
            while( pos < alLSPD.size ) {
                val lspd = alLSPD[ pos ]
                //--- сразу же пропускаем ненормальные периоды
                if( lspd.colorIndex != gh.lineNormalColorIndex ) {
                    pos++
                    continue
                }
                //--- определим несущественность расхода
                val begGDL = aLine.alGLD[ lspd.begPos ]
                val endGDL = aLine.alGLD[ lspd.endPos ]
                //--- заодно ловим периоды с нулевой длиной
                if( endGDL.x - begGDL.x < Math.max( sca.usingMinLen, 1 ) ) {
                    //--- ищем ненормальные периоды слева/справа для слияния
                    var prevAbnormalLSPD: LiquidStatePeriodData? = null
                    var nextAbnormalLSPD: LiquidStatePeriodData? = null
                    if( pos > 0 ) {
                        prevAbnormalLSPD = alLSPD[ pos - 1 ]
                        if( prevAbnormalLSPD.colorIndex == gh.lineNormalColorIndex ) prevAbnormalLSPD = null
                    }
                    if( pos < alLSPD.size - 1 ) {
                        nextAbnormalLSPD = alLSPD[ pos + 1 ]
                        if( nextAbnormalLSPD.colorIndex == gh.lineNormalColorIndex ) nextAbnormalLSPD = null
                    }

                    //--- оба соседних периода одинаково ненормальные, все три сливаем в один
                    //--- ( два соседних разно-ненормальных периода не сольёшь )
                    if( prevAbnormalLSPD != null && nextAbnormalLSPD != null && prevAbnormalLSPD.colorIndex == nextAbnormalLSPD.colorIndex ) {

                        prevAbnormalLSPD.endPos = nextAbnormalLSPD.endPos
                        alLSPD.removeAt( pos )
                        //--- это не опечатка и не ошибка: после удаления текущего периода следующий период
                        //--- становится текущим и тоже удаляется
                        alLSPD.removeAt( pos )
                        //--- после слияния трёх периодов pos уже указывает на следующую позицию,
                        //--- увеличивать счетчик не надо
                        //pos++;
                        //--- денормализуем "свои" точки сглаженного графика
                        for( i in lspd.begPos + 1..lspd.endPos ) aLine.alGLD[ i ].colorIndex = prevAbnormalLSPD.colorIndex
                    }
                    else pos++//--- иначе просто переходим к следущему периоду
                }
                else pos++//--- иначе просто переходим к следущему периоду
            }
        }

        //--- сглаживание графика аналоговой величины уровня жидкости/топлива ( сокращённый вызов для генерации отчётов )
        fun getSmoothLiquidGraphicData( alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, oc: ObjectConfig, sca: SensorConfigA, begTime: Int, endTime: Int,
                                        aLine: GraphicDataContainer, alLSPD: MutableList<LiquidStatePeriodData> ) {
            val gh = LiquidGraphicHandler()
            getSmoothAnalogGraphicData( alRawTime, alRawData, oc, sca, begTime, endTime, 0, 0.0, null, null, null, aLine, gh )
            getLiquidStatePeriodData( sca, aLine, alLSPD, gh )
        }

        fun calcLiquidUsingByLevel(sca: SensorConfigA, llcd: LiquidLevelCalcData, alMovingAndParking: List<AbstractPeriodData>?,
                                   stm: CoreAdvancedStatement, oc: ObjectConfig, begTime: Int, endTime: Int ) {
            val aLine = llcd.aLine
            val alLSPD = llcd.alLSPD

            if( !alLSPD!!.isEmpty() ) {
                //--- сначала считаем обычный расход
                for( i in alLSPD.indices ) {
                    val lspd = alLSPD[ i ]
                    val begGDL = aLine!!.alGLD[ lspd.begPos ]
                    val endGDL = aLine.alGLD[ lspd.endPos ]
                    when( lspd.colorIndex ) {
                        GraphicColorIndex.LINE_NORMAL_0   -> llcd.usingTotal += begGDL.y - endGDL.y
                        GraphicColorIndex.LINE_CRITICAL_0 -> {
                            llcd.incTotal += endGDL.y - begGDL.y
                            if( sca.isUsingCalc ) {
                                //--- ищем предыдущий нормальный период
                                val avgUsing = getPrevNormalPeriodAverageUsing( llcd, i, stm, oc, sca, begTime, endTime )
                                val calcUsing = avgUsing * ( endGDL.x - begGDL.x )
                                llcd.usingCalc = ( if( llcd.usingCalc == null ) 0.0 else llcd.usingCalc!! ) + calcUsing
                                llcd.usingTotal += calcUsing
                            }
                        }
                        GraphicColorIndex.LINE_WARNING_0  -> {
                            llcd.decTotal += begGDL.y - endGDL.y
                            if( sca.isUsingCalc ) {
                                //--- ищем предыдущий нормальный период
                                val avgUsing = getPrevNormalPeriodAverageUsing( llcd, i, stm, oc, sca, begTime, endTime )
                                val calcUsing = avgUsing * ( endGDL.x - begGDL.x )
                                llcd.usingCalc = ( if( llcd.usingCalc == null ) 0.0 else llcd.usingCalc!! ) + calcUsing
                                llcd.usingTotal += calcUsing
                            }
                        }
                    }
                }

                //--- если заданы периоды движения/стоянки, то отдельно раскидаем заправку/слив/расход по периодам
                //--- движения/стоянки, не забывая про вычисляемый расход во время заправки/стоянки на основе averageUsingSpeed
                if( alMovingAndParking != null && !alMovingAndParking.isEmpty() ) {
                    //--- для процедуры пересечения/умножения периодов перегрузим
                    //--- LiquidStatePeriodData в LiquidLevelPeriodData
                    val alLLPD = mutableListOf<AbstractPeriodData>()
                    for( lspd in alLSPD ) alLLPD.add( LiquidLevelPeriodData( aLine!!.alGLD[ lspd.begPos ].x, aLine.alGLD[ lspd.endPos ].x, lspd.colorIndex ) )
                    //--- пересекаем/умножаем два списка периодов
                    val alMPD = multiplePeriods( alMovingAndParking, alLLPD )
                    //--- пробегаем по списку пересечения
                    for( mpd in alMPD ) {
                        //--- в движении
                        if( mpd.state1 != 0 ) {
                            when( mpd.state2 ) {
                                /*GraphicColorIndex.LINE_NORMAL_0*/ 1 -> llcd.usingMoving += searchGDL( aLine!!, mpd.begTime ) - searchGDL( aLine, mpd.endTime )
                            }//                        case GraphicColorIndex.LINE_CRITICAL_0:
                            //                            //--- не забываем про начисление расхода по среднему за период заправки/слива
                            //                            llcd.calcUsingMoving += averageUsingSpeed * (  mpd.endTime - mpd.begTime  );
                            //                            break;
                            //                        case GraphicColorIndex.LINE_WARNING_0:
                            //                            //--- не забываем про начисление расхода по среднему за период заправки/слива
                            //                            llcd.calcUsingMoving += averageUsingSpeed * (  mpd.endTime - mpd.begTime  );
                            //                            break;
                        }
                        else {
                            when( mpd.state2 ) {
                                /*GraphicColorIndex.LINE_NORMAL_0*/ 1 -> llcd.usingParking += searchGDL( aLine!!, mpd.begTime ) - searchGDL( aLine, mpd.endTime )
                            }//                        case GraphicColorIndex.LINE_CRITICAL_0:
                            //                            //--- не забываем про начисление расхода по среднему за период заправки/слива
                            //                            llcd.calcUsingParking += averageUsingSpeed * (  mpd.endTime - mpd.begTime  );
                            //                            break;
                            //                        case GraphicColorIndex.LINE_WARNING_0:
                            //                            //--- не забываем про начисление расхода по среднему за период заправки/слива
                            //                            llcd.calcUsingParking += averageUsingSpeed * (  mpd.endTime - mpd.begTime  );
                            //                            break;
                        }//--- на стоянке
                    }
                    //--- если будет использоваться расчётный расход во время заправки/слива,
                    //--- то прибавляем его к стояночному расходу
                    //--- ( исходим из того, что заправки/сливы происходят только на стоянках )
                    if( sca.isUsingCalc ) llcd.usingParking += if( llcd.usingCalc == null ) 0.0 else llcd.usingCalc!!
                }
            }
        }

        //--- ищем предыдущий нормальный период для расчёта среднего расхода во время заправки/слива
        private fun getPrevNormalPeriodAverageUsing(llcd: LiquidLevelCalcData, curPos: Int,
                                                    stm: CoreAdvancedStatement, oc: ObjectConfig, sca: SensorConfigA, begTime: Int, endTime: Int ): Double {

            var lspdPrevNorm: LiquidStatePeriodData? = null
            var aLinePrevNorm: GraphicDataContainer? = null

            val aLine = llcd.aLine
            val alLSPD = llcd.alLSPD

            for( i in curPos - 1 downTo 0 ) {
                val lspdPrev = alLSPD!![ i ]
                val begGDLPrev = aLine!!.alGLD[ lspdPrev.begPos ]
                val endGDLPrev = aLine.alGLD[ lspdPrev.endPos ]

                //--- найденный нормальный период не является первым ( неважно какой продолжительности ) или первым,
                //--- но с достаточной для вычислений продолжительностью
                if( lspdPrev.colorIndex == GraphicColorIndex.LINE_NORMAL_0 && ( i > 0 || endGDLPrev.x - begGDLPrev.x >= MAX_CALC_PREV_NORMAL_PERIOD ) ) {

                    lspdPrevNorm = lspdPrev
                    aLinePrevNorm = aLine
                    break
                }
            }
            //--- подходящий нормальный участок во всём запрашиваемом периоде не найден - запрашиваем расширенный период
            if( lspdPrevNorm == null ) {
                //--- расширим период в прошлое с двухкратным запасом - на скорость обработки это не сильно повлияет
                val ( alRawTimeExt, alRawDataExt ) = loadAllSensorData( stm, oc, begTime - MAX_CALC_PREV_NORMAL_PERIOD * 2, endTime )

                val aLineExt = GraphicDataContainer( GraphicDataContainer.ElementType.LINE, 0, 2 )
                val alLSPDExt = mutableListOf<LiquidStatePeriodData>()
                getSmoothLiquidGraphicData( alRawTimeExt, alRawDataExt, oc, sca, begTime - MAX_CALC_PREV_NORMAL_PERIOD * 2, endTime, aLineExt, alLSPDExt )

                //--- текущий период в текущем диапазоне
                val lspdCur = alLSPD!![ curPos ]
                val begGDLPCur = aLine!!.alGLD[ lspdCur.begPos ]
                val endGDLPCur = aLine.alGLD[ lspdCur.endPos ]
                //--- найдём текущий период заправки/слива в новом расширенном периоде
                var curPosExt = 0
                while( curPosExt < alLSPDExt.size ) {
                    val lspdCurExt = alLSPDExt[ curPosExt ]
                    val begGDLPCurExt = aLineExt.alGLD[ lspdCurExt.begPos ]
                    val endGDLPCurExt = aLineExt.alGLD[ lspdCurExt.endPos ]
                    if( begGDLPCur.x == begGDLPCurExt.x && endGDLPCur.x == endGDLPCurExt.x ) break
                    curPosExt++
                }
                for( i in curPosExt - 1 downTo 0 ) {
                    val lspdPrevExt = alLSPDExt[ i ]
                    if( lspdPrevExt.colorIndex == GraphicColorIndex.LINE_NORMAL_0 ) {
                        lspdPrevNorm = lspdPrevExt
                        aLinePrevNorm = aLineExt
                        break
                    }
                }
            }
            //--- посчитаем таки средний расход в предыдущем нормальном периоде
            if( lspdPrevNorm != null ) {
                var begGDLPrevNorm = aLinePrevNorm!!.alGLD[ lspdPrevNorm.begPos ]
                val endGDLPrevNorm = aLinePrevNorm.alGLD[ lspdPrevNorm.endPos ]
                //--- нормальный период слишком большой, берём последние N часов - корректируем begPos
                if( endGDLPrevNorm.x - begGDLPrevNorm.x > MAX_CALC_PREV_NORMAL_PERIOD ) {
                    for( begPos in lspdPrevNorm.begPos + 1 until lspdPrevNorm.endPos ) {
                        begGDLPrevNorm = aLinePrevNorm.alGLD[ begPos ]
                        if( endGDLPrevNorm.x - begGDLPrevNorm.x <= MAX_CALC_PREV_NORMAL_PERIOD ) break
                    }
                }

                return if( endGDLPrevNorm.x == begGDLPrevNorm.x ) 0.0 else ( begGDLPrevNorm.y - endGDLPrevNorm.y ) / ( endGDLPrevNorm.x - begGDLPrevNorm.x )
            }
            else return 0.0
        }

        //--- собираем периоды, величины и место заправок/сливов
        fun calcIncDec(stm: CoreAdvancedStatement, alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, oc: ObjectConfig, sca: SensorConfigA,
                       begTime: Int, endTime: Int, isWaybill: Boolean, alBeg: List<Int>, alEnd: List<Int>, calcMode: Int,
                       hmZoneData: Map<Int, ZoneData>, calcZoneID: Int ): List<LiquidIncDecData> {
            val alLIDD = mutableListOf<LiquidIncDecData>()

            val aLine = GraphicDataContainer( GraphicDataContainer.ElementType.LINE, 0, 2 )
            val alLSPD = mutableListOf<LiquidStatePeriodData>()
            getSmoothLiquidGraphicData( alRawTime, alRawData, oc, sca, begTime, endTime, aLine, alLSPD )

            val llcd = LiquidLevelCalcData( aLine, alLSPD )
            calcLiquidUsingByLevel( sca, llcd, null, stm, oc, begTime, endTime )

            for( lspd in llcd.alLSPD!! ) {
                val begGLD = llcd.aLine!!.alGLD[ lspd.begPos ]
                val endGLD = llcd.aLine!!.alGLD[ lspd.endPos ]
                var lidd: LiquidIncDecData? = null
                if( lspd.colorIndex == GraphicColorIndex.LINE_CRITICAL_0 && calcMode >= 0 ) lidd = LiquidIncDecData( begGLD.x, endGLD.x, begGLD.y, endGLD.y )
                else if( lspd.colorIndex == GraphicColorIndex.LINE_WARNING_0 && calcMode <= 0 ) lidd = LiquidIncDecData( begGLD.x, endGLD.x, begGLD.y, endGLD.y )

                if( lidd != null ) {
                    var inZoneAll = false
                    val tsZoneName = TreeSet<String>()
                    if( oc.scg != null ) for( pos in lspd.begPos..lspd.endPos ) {
                        val gd = AbstractObjectStateCalc.getGeoData( oc, alRawData[ pos ] ) ?: continue
                        //--- самих геоданных может и не оказаться
                        val pixPoint = XyProjection.wgs_pix( gd.wgs )

                        val inZone = fillZoneList( hmZoneData, calcZoneID, pixPoint, tsZoneName )
                        //--- фильтр по геозонам, если задано
                        if( calcZoneID != 0 && inZone ) inZoneAll = true
                    }
                    //--- фильтр по геозонам, если задано
                    if( calcZoneID != 0 && !inZoneAll ) continue

                    //--- фильтр по времени путевого листа, если задано
                    if( isWaybill ) {
                        var inWaybill = false
                        for( wi in alBeg.indices ) if( lidd.begTime < alEnd[ wi ] && lidd.endTime > alBeg[ wi ] ) {
                            inWaybill = true
                            break
                        }
                        if( inWaybill ) continue
                    }

                    lidd.objectConfig = oc
                    lidd.sca = sca
                    lidd.sbZoneName = getSBFromIterable( tsZoneName, ", " )

                    alLIDD.add( lidd )
                }
            }

            return alLIDD
        }

        //--- универсальная функция определения РЕАЛЬНОЙ суммы значений счетчика
        private fun getSensorCountData( alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, oc: ObjectConfig, scu: SensorConfigU, begTime: Int, endTime: Int ): Double {
            //--- проход по диапазону
            var sensorSum = 0.0
            for( pos in alRawTime.indices ) {
                val rawTime = alRawTime[ pos ]
                //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
                if( rawTime < begTime ) continue
                if( rawTime > endTime ) break

                val sensorData = when(val rawSensorData = AbstractObjectStateCalc.getSensorData( oc, scu.portNum, alRawData[ pos ] )) {
                    is Int -> {
                        rawSensorData.toDouble()
                    }
                    is Double -> {
                        rawSensorData
                    }
                    else -> {
                        0.0
                    }
                }
                //--- вручную игнорируем заграничные значения
                if( sensorData < scu.minIgnore || sensorData > scu.maxIgnore ) continue

                sensorSum += sensorData
            }
            return sensorSum * scu.dataValue / scu.sensorValue
        }

        private fun searchGDL( aLine: GraphicDataContainer, time: Int ): Double {
            //--- если время находится на/за поисковыми границами, то берём граничное значение
            if( time <= aLine.alGLD[ 0 ].x ) return aLine.alGLD[ 0 ].y
            else if( time >= aLine.alGLD[ aLine.alGLD.size - 1 ].x ) return aLine.alGLD[ aLine.alGLD.size - 1 ].y

            var pos1 = 0
            var pos2 = aLine.alGLD.size - 1
            while( pos1 <= pos2 ) {
                val posMid = ( pos1 + pos2 ).ushr( 1 )
                val valueMid = aLine.alGLD[ posMid ].x

                if( time < valueMid ) pos2 = posMid - 1
                else if( time > valueMid ) pos1 = posMid + 1
                else return aLine.alGLD[ posMid ].y
            }
            //--- если ничего не нашли, то теперь pos2 - левее искомого значения, а pos1 - правее его.
            //--- в этом случае аппроксимируем значение
            return ( time - aLine.alGLD[ pos2 ].x ) / ( aLine.alGLD[ pos1 ].x - aLine.alGLD[ pos2 ].x ) * ( aLine.alGLD[ pos1 ].y - aLine.alGLD[ pos2 ].y ) + aLine.alGLD[ pos2 ].y
        }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        //--- заполнение типовых строк вывода ( для табличных форм и отчетов )
        fun fillGeoString(gcd: GeoCalcData, zoneId: ZoneId, sbGeoName: StringBuilder, sbGeoRun: StringBuilder, sbGeoOutTime: StringBuilder, sbGeoInTime: StringBuilder,
                          sbGeoWayTime: StringBuilder, sbGeoMovingTime: StringBuilder, sbGeoParkingTime: StringBuilder, sbGeoParkingCount: StringBuilder ) {
            sbGeoName.append( gcd.descr )
            sbGeoRun.append( if( gcd.run < 0 ) '-' else getSplittedDouble( gcd.run, 1 ) )
            sbGeoOutTime.append( if( gcd.outTime == 0 ) "-" else DateTime_DMYHMS( zoneId, gcd.outTime ) )
            sbGeoInTime.append( if( gcd.inTime == 0 ) "-" else DateTime_DMYHMS( zoneId, gcd.inTime ) )
            sbGeoWayTime.append( if( gcd.outTime == 0 || gcd.inTime == 0 ) '-' else secondIntervalToString( gcd.outTime, gcd.inTime ))
            sbGeoMovingTime.append( if( gcd.movingTime < 0 ) '-' else secondIntervalToString( gcd.movingTime ))
            sbGeoParkingTime.append( if( gcd.parkingTime < 0 ) '-' else secondIntervalToString( gcd.parkingTime ))
            sbGeoParkingCount.append( if( gcd.parkingCount < 0 ) '-' else getSplittedLong( gcd.parkingCount.toLong() ) )
        }

        fun fillZoneString( hmZoneData: Map<Int, ZoneData>, p: XyPoint): StringBuilder {
            val tsZoneName = TreeSet<String>()
            for( zd in hmZoneData.values )
                if( zd.polygon!!.isContains( p ) )
                    tsZoneName.add( zd.name )
            return getSBFromIterable( tsZoneName, ", " )
        }

        fun fillWorkString( tmWorkCalc: TreeMap<String, WorkCalcData>, sbWorkName: StringBuilder, sbWorkTotal: StringBuilder, sbWorkMoving: StringBuilder, sbWorkParking: StringBuilder ) {

            for( ( workName, wcd ) in tmWorkCalc ) {
                if( !sbWorkName.isEmpty() ) {
                    sbWorkName.append( '\n' )
                    sbWorkTotal.append( '\n' )
                    sbWorkMoving.append( '\n' )
                    sbWorkParking.append( '\n' )
                }
                sbWorkName.append( workName )
                sbWorkTotal.append( getSplittedDouble( wcd.onTime.toDouble() / 60.0 / 60.0, 1 ) )
                sbWorkMoving.append( getSplittedDouble( wcd.onMovingTime.toDouble() / 60.0 / 60.0, 1 ) )
                sbWorkParking.append( getSplittedDouble( wcd.onParkingTime.toDouble() / 60.0 / 60.0, 1 ) )
            }
        }

        fun fillLiquidLevelString( tmLiquidLevelCalc: TreeMap<String, LiquidLevelCalcData>,
                                   sbLiquidLevelName: StringBuilder, sbLiquidLevelBeg: StringBuilder, sbLiquidLevelEnd: StringBuilder,
                                   sbLiquidLevelIncTotal: StringBuilder, sbLiquidLevelDecTotal: StringBuilder,
                                   sbLiquidLevelUsingTotal: StringBuilder, sbLiquidLevelUsingMoving: StringBuilder, sbLiquidLevelUsingParking: StringBuilder,
                                   sbLiquidLevelUsingCalc: StringBuilder ) {

            //--- используется ли вообще usingCalc
            var isUsingCalc = false
            for( llcd in tmLiquidLevelCalc.values )
                if( llcd.usingCalc != null ) {
                    isUsingCalc = true
                    break
                }

            for( ( liquidName, llcd ) in tmLiquidLevelCalc ) {
                if( !sbLiquidLevelName.isEmpty() ) {
                    sbLiquidLevelName.append( '\n' )
                    sbLiquidLevelBeg.append( '\n' )
                    sbLiquidLevelEnd.append( '\n' )
                    sbLiquidLevelIncTotal.append( '\n' )
                    sbLiquidLevelDecTotal.append( '\n' )
                    sbLiquidLevelUsingTotal.append( '\n' )
                    sbLiquidLevelUsingMoving.append( '\n' )
                    sbLiquidLevelUsingParking.append( '\n' )
                    if( isUsingCalc ) sbLiquidLevelUsingCalc.append( '\n' )
                }
                sbLiquidLevelName.append( liquidName )
                sbLiquidLevelBeg.append( getSplittedDouble( llcd.begLevel, getPrecision( llcd.begLevel ) ) )
                sbLiquidLevelEnd.append( getSplittedDouble( llcd.endLevel, getPrecision( llcd.endLevel ) ) )
                sbLiquidLevelIncTotal.append( getSplittedDouble( llcd.incTotal, getPrecision( llcd.incTotal ) ) )
                sbLiquidLevelDecTotal.append( getSplittedDouble( llcd.decTotal, getPrecision( llcd.decTotal ) ) )
                sbLiquidLevelUsingTotal.append( getSplittedDouble( llcd.usingTotal, getPrecision( llcd.usingTotal ) ) )
                sbLiquidLevelUsingMoving.append( getSplittedDouble( llcd.usingMoving, getPrecision( llcd.usingMoving ) ) )
                sbLiquidLevelUsingParking.append( getSplittedDouble( llcd.usingParking, getPrecision( llcd.usingParking ) ) )
                if( isUsingCalc ) sbLiquidLevelUsingCalc.append( if( llcd.usingCalc == null ) "-"
                else getSplittedDouble( llcd.usingCalc!!, getPrecision( llcd.usingCalc!! ) ) )
            }
        }

        fun fillLiquidUsingString( tmLiquidUsingCalc: TreeMap<String, LiquidUsingCalcData>,
                                   sbLiquidUsingName: StringBuilder, sbLiquidUsingTotal: StringBuilder, sbLiquidUsingInMove: StringBuilder, sbLiquidUsingInParking: StringBuilder ) {
            for( ( liquidName, lucd ) in tmLiquidUsingCalc ) {
                if( !sbLiquidUsingName.isEmpty() ) {
                    sbLiquidUsingName.append( '\n' )
                    sbLiquidUsingTotal.append( '\n' )
                    sbLiquidUsingInMove.append( '\n' )
                    sbLiquidUsingInParking.append( '\n' )
                }
                sbLiquidUsingName.append( liquidName )
                sbLiquidUsingTotal.append( getSplittedDouble( lucd.usingTotal, getPrecision( lucd.usingTotal ) ) )
                sbLiquidUsingInMove.append( getSplittedDouble( lucd.usingMoving, getPrecision( lucd.usingMoving ) ) )
                sbLiquidUsingInParking.append( getSplittedDouble( lucd.usingParking, getPrecision( lucd.usingParking ) ) )
            }
        }

        fun fillEnergoString( tmEnergoCalc: TreeMap<String, Int>, sbEnergoName: StringBuilder, sbEnergoValue: StringBuilder ) {
            for( ( energoName, e ) in tmEnergoCalc ) {
                if( sbEnergoName.isNotEmpty() ) {
                    sbEnergoName.append( '\n' )
                    sbEnergoValue.append( '\n' )
                }
                sbEnergoName.append( energoName )
                //--- выводим в кВт*ч
                sbEnergoValue.append( getSplittedDouble( e / 1000.0, 3 ) )
            }
        }

        fun getPrecision( value: Double ): Int {
            //--- обновлённый/упрощённый вариант точности вывода - больше кубометра - в целых литрах, менее - в сотнях миллилитров/грамм
            return if( value >= 1000 ) 0 else 1
            //        return value >= 1000 ? 0
            //                             : value >= 100 ? 1
            //                                            : 2;
        }

        //----------------------------------------------------------------------------------------------------------------------------------------

        fun fillZoneList(hmZoneData: Map<Int, ZoneData>, reportZone: Int, p: XyPoint, tsZoneName: TreeSet<String> ): Boolean {
            var inZone = false
            for( ( zoneID, zd ) in hmZoneData )
                if( zd.polygon!!.isContains( p ) ) {
                    val sbZoneInfo = StringBuilder( zd.name )
                    if( /*zd.descr != null &&*/ !zd.descr.isEmpty() ) sbZoneInfo.append( " (" ).append( zd.descr ).append( ')' )

                    tsZoneName.add( sbZoneInfo.toString() )
                    //--- фильтр по геозонам, если задано
                    if( reportZone != 0 && reportZone == zoneID ) inZone = true
                }
            return inZone
        }
    }
}

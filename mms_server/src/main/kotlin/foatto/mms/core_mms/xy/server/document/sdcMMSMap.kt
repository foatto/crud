package foatto.mms.core_mms.xy.server.document

import foatto.app.CoreSpringController
import foatto.core.app.PARAM_ZONE_DESCR
import foatto.core.app.PARAM_ZONE_NAME
import foatto.core.app.iCoreAppContainer
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyElement
import foatto.core.app.xy.XyProjection
import foatto.core.app.xy.geom.XyPoint
import foatto.core.link.XyDocumentConfig
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getRandomInt
import foatto.core.util.getSplittedDouble
import foatto.core.util.prepareForSQL
import foatto.core.util.sLineSeparator
import foatto.core.util.secondIntervalToString
import foatto.core_server.app.AppParameter
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectData
import foatto.core_server.app.xy.XyStartObjectParsedData
import foatto.core_server.app.xy.server.XyProperty
import foatto.core_server.app.xy.server.document.sdcXyMap
import foatto.mms.MMSSpringController
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.ZoneData
import foatto.mms.core_mms.ZoneLimitData
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.GeoPeriodData
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.calc.ObjectState
import foatto.mms.core_mms.calc.OverSpeedPeriodData
import foatto.mms.core_mms.sensor.SensorConfig
import foatto.mms.core_mms.sensor.SensorConfigWork
import foatto.sql.CoreAdvancedStatement
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

class sdcMMSMap : sdcXyMap() {

    companion object {
        const val ELEMENT_TYPE_ZONE = "mms_zone"

        //--- располагаем по возрастанию уровня расположения
        val TYPE_OBJECT_TRACE = "mms_object_trace"
        val TYPE_OBJECT_PARKING = "mms_object_parking"
        val TYPE_OBJECT_OVER_SPEED = "mms_object_over_speed"
        val TYPE_OBJECT_TRACE_INFO = "mms_object_trace_info"
        val TYPE_OBJECT_INFO = "mms_object_info"

        fun getZoneTooltip(zoneName: String, zoneDescr: String) = prepareForSQL(" Геозона \n Наименование: $zoneName \n Описание: $zoneDescr \n")
    }

    //--- "линейная" зона существует только на клиенте в момент её создания на карте
    //public static final String TYPE_ZONE_LINEAR = "mms_zone_linear";

    private lateinit var hmZoneData: Map<Int, ZoneData>

    override fun init(aAppController: CoreSpringController, aStm: CoreAdvancedStatement, aChmSession: ConcurrentHashMap<String, Any>, aUserConfig: UserConfig, aDocumentConfig: XyDocumentConfig) {

        //--- зоны ---

        hmInAliasElementType["mms_zone"] = arrayOf(ELEMENT_TYPE_ZONE)

        hmOutElementTypeAlias[ELEMENT_TYPE_ZONE] = "mms_zone"

        //--- траектории ---

        hmInAliasElementType["mms_object"] = arrayOf(TYPE_OBJECT_TRACE, TYPE_OBJECT_PARKING, TYPE_OBJECT_OVER_SPEED, TYPE_OBJECT_TRACE_INFO, TYPE_OBJECT_INFO)

        hmOutElementTypeAlias[TYPE_OBJECT_TRACE] = "mms_object"
        hmOutElementTypeAlias[TYPE_OBJECT_PARKING] = "mms_object"
        hmOutElementTypeAlias[TYPE_OBJECT_OVER_SPEED] = "mms_object"
        hmOutElementTypeAlias[TYPE_OBJECT_TRACE_INFO] = "mms_object"
        hmOutElementTypeAlias[TYPE_OBJECT_INFO] = "mms_object"

        //------

        super.init(aAppController, aStm, aChmSession, aUserConfig, aDocumentConfig)
    }

    override fun getCoords(startParamID: String): XyActionResponse {
        val sd = chmSession[AppParameter.XY_START_DATA + startParamID] as XyStartData

        var isFound = false
        val minXY = XyPoint(Integer.MAX_VALUE, Integer.MAX_VALUE)
        val maxXY = XyPoint(-Integer.MAX_VALUE, -Integer.MAX_VALUE)

        //--- сначала проверим, есть ли стартовые объекты
        val isStartObjectsDefined = sd.alStartObjectData.any { it.isStart }

        //--- разбор входных параметров
        val alObjectParamData = parseObjectParam(isStartObjectsDefined, sd, mutableSetOf())

        //--- отдельная групповая обработка статических объектов
        val sbStaticObjectID = StringBuilder()
        for(objectParamData in alObjectParamData) {
            //--- статические объекты - зоны
            if(objectParamData.begTime == 0) sbStaticObjectID.append(if(sbStaticObjectID.isEmpty()) "" else " , ").append(objectParamData.objectID)
        }
        if(!sbStaticObjectID.isEmpty()) {
            //--- именно MIN/MAX, т.к. одному статическому объекту может соответствовать несколько элементов
            //--- (нельзя убирать проверку на COUNT, т.к. при отсутствии выборки все равно вернется строка с null-значениями)
            val inRs = stm.executeQuery(
                " SELECT COUNT( * ) , MIN( prj_x1 ) , MIN( prj_y1 ) , MAX( prj_x2 ) , MAX( prj_y2 ) FROM XY_element WHERE object_id IN ( $sbStaticObjectID ) "
            )
            if(inRs.next() && inRs.getInt(1) > 0) {
                getMinMaxCoords(XyPoint(inRs.getInt(2), inRs.getInt(3)), minXY, maxXY)
                getMinMaxCoords(XyPoint(inRs.getInt(4), inRs.getInt(5)), minXY, maxXY)
                isFound = true
            }
            inRs.close()
        }

        //--- отдельная обработка динамических объектов
        for(objectParamData in alObjectParamData) {
            //--- статические объекты уже обработаны
            if(objectParamData.begTime == 0) continue
            //--- нужен хотя бы один гео-датчик
            val objectConfig = ObjectConfig.getObjectConfig(stm, userConfig, objectParamData.objectID)
            if(objectConfig.scg == null) continue

            //--- если траекторные элементы глобально не запрещены
            if(objectParamData.begTime < objectParamData.endTime) {
                val inRs = stm.executeQuery(
                    " SELECT sensor_data FROM MMS_data_${objectParamData.objectID} WHERE ontime >= ${objectParamData.begTime} AND ontime <= ${objectParamData.endTime} "
                )
                while(inRs.next()) {
                    val bbSensor = inRs.getByteBuffer(1, ByteOrder.BIG_ENDIAN)
                    val gd = AbstractObjectStateCalc.getGeoData(objectConfig, bbSensor) ?: continue
                    //--- самих геоданных в этой строке может и не оказаться
                    getMinMaxCoords(XyProjection.wgs_pix(gd.wgs), minXY, maxXY)
                    isFound = true
                }
                inRs.close()
            }
            //--- если по траектории ничего не нашлось, пробуем по последнему положению объекта
            if(!isFound) {
                val objectState = ObjectState.getState(stm, objectConfig)
                //--- если текущее положение не в будущем относительно запрашиваемого периода и есть последние координаты
                if(objectState.time <= objectParamData.endTime && objectState.pixPoint != null) {
                    getMinMaxCoords(objectState.pixPoint!!, minXY, maxXY)
                    isFound = true
                }
            }
        }

        //--- если элементы, соответствующие стартовым объектам, не нашлись, то возвращаем координаты России
        return XyActionResponse(
            minCoord = if(isFound) minXY else XyProjection.wgs_pix(20, 70),
            maxCoord = if(isFound) maxXY else XyProjection.wgs_pix(180, 40)
        )
    }

    override fun getElements(xyActionRequest: XyActionRequest): XyActionResponse {

        hmZoneData = ZoneData.getZoneData(stm, userConfig, 0)

        return super.getElements(xyActionRequest)
    }

    override fun clickElement(xyActionRequest: XyActionRequest) = XyActionResponse()

    override fun addElement(xyActionRequest: XyActionRequest, userID: Int): XyActionResponse {
        val xyStartDataID = xyActionRequest.startParamID
        val sd = chmSession[AppParameter.XY_START_DATA + xyStartDataID] as XyStartData

        val xyElement = xyActionRequest.xyElement!!
        xyElement.elementID = stm.getNextID("XY_element", "id")
        xyElement.objectID = stm.getNextID(arrayOf("MMS_object", "MMS_zone"), arrayOf("id", "id"))

        val zoneName = xyActionRequest.hmParam[PARAM_ZONE_NAME] ?: "-"
        val zoneDescr = xyActionRequest.hmParam[PARAM_ZONE_DESCR] ?: "-"

        stm.executeUpdate(
            " INSERT INTO MMS_zone ( id , user_id , name , descr, outer_id ) VALUES ( ${xyElement.objectID} , $userID , '${prepareForSQL(zoneName)}' , '${prepareForSQL(zoneDescr)}' , '' ) "
        )

        putElement(xyElement, true)

        stm.executeUpdate(
            " INSERT INTO XY_property ( element_id , property_name , property_value ) VALUES ( " +
                "${xyElement.elementID} , '${XyProperty.TOOL_TIP_TEXT}' , '${getZoneTooltip(zoneName, zoneDescr)}' ) "
        )

//        //Integer routeID = (Integer) hmParam.get( XyParameter.PARENT_OBJECT_ID );
//        //linkToRoute( arrConn, stm, hmParam, routeID, zoneObjectID );

        sd.alStartObjectData.add(XyStartObjectData(xyElement.objectID, "mms_zone", false, false, false))

        return XyActionResponse()
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun loadDynamicElements(scale: Int, objectParamData: XyStartObjectParsedData, alElement: MutableList<XyElement>) {
        val hmZoneLimit = ZoneLimitData.getZoneLimit(
            stm, userConfig, hmZoneData, objectParamData.objectID,
            /*0, objectParamData.begDT, objectParamData.endDT,*/ ZoneLimitData.TYPE_LIMIT_SPEED
        )

        val objectConfig = ObjectConfig.getObjectConfig(stm, userConfig, objectParamData.objectID)
        if(objectConfig.scg == null) return

        val objectNameAndModel = StringBuilder(objectConfig.name).append(if(objectConfig.model.isEmpty()) "" else ", ").append(objectConfig.model).toString()

        //--- если траекторные элементы глобально не запрещены
        if(objectParamData.begTime < objectParamData.endTime) {
            val maxEnabledOverSpeed = (appController as MMSSpringController).maxEnabledOverSpeed

            //--- грузим данные за период
            val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(stm, objectConfig, objectParamData.begTime, objectParamData.endTime)
            //--- Хоть какая-то экономия трафика. ( 16 - достаточное огрубление - и стрелки видно и не слишком грубо срезано )
            val gcd = ObjectCalc.calcGeoSensor(
                alRawTime, alRawData, objectConfig, objectParamData.begTime, objectParamData.endTime, scale * 16, maxEnabledOverSpeed,
                hmZoneLimit[ZoneLimitData.TYPE_LIMIT_SPEED]
            )

            //--- генерация траектории, стоянок и превышений скорости ---

            //--- вывод траектории
            if(!gcd.alPointXY.isNullOrEmpty() && !gcd.alPointSpeed.isNullOrEmpty() && !gcd.alPointOverSpeed.isNullOrEmpty()) {

                //--- определим начало/окончание работы для Т1/Т2 на траектории
                val tmWorkBegTime = TreeMap<String, Int>()
                val tmWorkEndTime = TreeMap<String, Int>()
                val hmSCW = objectConfig.hmSensorConfig[SensorConfig.SENSOR_WORK]
                if(hmSCW != null && !hmSCW.isEmpty())
                    for(portNum in hmSCW.keys) {
                        val scw = hmSCW[portNum] as SensorConfigWork

                        val alWork = ObjectCalc.calcWorkSensor(alRawTime, alRawData, objectConfig, scw, objectParamData.begTime, objectParamData.endTime, mutableListOf()).alWorkOnOff
                        if(alWork != null)
                            for(apd in alWork)
                                if(apd.getState() != 0) {
                                    if(tmWorkBegTime[scw.descr] == null) tmWorkBegTime[scw.descr] = apd.begTime
                                    tmWorkEndTime[scw.descr] = apd.endTime
                                }
                    }

                val objectTrace = XyElement(TYPE_OBJECT_TRACE, -getRandomInt(), objectParamData.objectID)
                //objectTrace.init( zoneId )
                objectTrace.toolTipText = objectNameAndModel
                objectTrace.itReadOnly = true
                objectTrace.lineWidth = 3
                objectTrace.arrowPos = XyElement.ArrowPos.MIDDLE
                //--- оптимальная длина стрелки = 3 ширинам стрелки ( допустим диапазон от 2 до 4 ширин стрелки )
                objectTrace.arrowLen = 9
                //--- стрелки высотой крыльев ( шириной стрелки ) менее 3 пикселов уже неразличимы
                objectTrace.arrowHeight = 3
                objectTrace.arrowLineWidth = 1

                //--- эти данные касаются межточечных отрезков, их на 1 меньше
                for(i in 0 until gcd.alPointXY!!.size - 1) {
                    val overSpeed = gcd.alPointOverSpeed!![i]
                    val sb = "$objectNameAndModel$sLineSeparator${DateTime_DMYHMS(zoneId, gcd.alPointTime!![i])} -> ${DateTime_DMYHMS(zoneId, gcd.alPointTime!![i + 1])}" +
                        "$sLineSeparator${gcd.alPointSpeed!![i]} -> ${gcd.alPointSpeed!![i + 1]} км/ч"

                    objectTrace.addTracePoint(gcd.alPointXY!![i], if(overSpeed > maxEnabledOverSpeed) 0xFF_FF_00_00.toInt() else 0xFF_00_00_FF.toInt(), sb)
                }
                //--- последняя точка добавляется без траекторной информации
                objectTrace.alPoint.add(gcd.alPointXY!![gcd.alPointXY!!.size - 1])

                alElement.add(objectTrace)

                //--- знак начала траектории
                val objectTraceText1 = XyElement(TYPE_OBJECT_TRACE_INFO, -getRandomInt(), objectParamData.objectID)
                //objectTraceText1.init( zoneId )
                objectTraceText1.itReadOnly = true
                objectTraceText1.alPoint.add(gcd.alPointXY!![0])
                objectTraceText1.anchorX = XyElement.Anchor.LT
                objectTraceText1.anchorY = XyElement.Anchor.LT
                objectTraceText1.fillColor = 0xFFFFFFFF.toInt()
                objectTraceText1.drawColor = 0xFF000000.toInt()
                objectTraceText1.lineWidth = 1

                val sbT1 = StringBuilder(objectNameAndModel).append(sLineSeparator).append("Начало траектории:").append(sLineSeparator)
                    .append(DateTime_DMYHMS(zoneId, gcd.alPointTime!![0])).append(sLineSeparator).append(gcd.alPointSpeed!![0]).append(" км/ч")
                if(!tmWorkBegTime.isEmpty()) sbT1.append(sLineSeparator).append("Начало работы оборудования:")
                for((descr, bt) in tmWorkBegTime)
                    sbT1.append(sLineSeparator).append(descr).append(": ").append(DateTime_DMYHMS(zoneId, bt))

                objectTraceText1.text = "T1"
                objectTraceText1.toolTipText = sbT1.toString()
                objectTraceText1.textColor = 0xFF000000.toInt()
                objectTraceText1.fontSize = iCoreAppContainer.BASE_FONT_SIZE
                objectTraceText1.itFontBold = true

                alElement.add(objectTraceText1)

                //--- знак конца траектории
                val objectTraceText2 = XyElement(TYPE_OBJECT_TRACE_INFO, -getRandomInt(), objectParamData.objectID)
                //objectTraceText2.init( zoneId )
                objectTraceText2.itReadOnly = true
                objectTraceText2.alPoint.add(gcd.alPointXY!![gcd.alPointXY!!.size - 1])
                objectTraceText2.anchorX = XyElement.Anchor.LT
                objectTraceText2.anchorY = XyElement.Anchor.LT
                objectTraceText2.fillColor = 0xFFFFFFFF.toInt()
                objectTraceText2.drawColor = 0xFF000000.toInt()
                objectTraceText2.lineWidth = 1

                val sbT2 = StringBuilder(objectNameAndModel).append(sLineSeparator).append("Окончание траектории").append(sLineSeparator)
                    .append(DateTime_DMYHMS(zoneId, gcd.alPointTime!![gcd.alPointTime!!.size - 1])).append(sLineSeparator)
                    .append(gcd.alPointSpeed!![gcd.alPointSpeed!!.size - 1]).append(" км/ч")
                if(!tmWorkEndTime.isEmpty()) sbT2.append(sLineSeparator).append("Окончание работы оборудования:")
                for((descr, et) in tmWorkEndTime)
                    sbT2.append(sLineSeparator).append(descr).append(": ").append(DateTime_DMYHMS(zoneId, et))

                objectTraceText2.text = "T2"
                objectTraceText2.toolTipText = sbT2.toString()
                objectTraceText2.textColor = 0xFF000000.toInt()
                objectTraceText2.fontSize = iCoreAppContainer.BASE_FONT_SIZE
                objectTraceText2.itFontBold = true

                alElement.add(objectTraceText2)
            }
            //--- вывод стоянок
            if(gcd.alMovingAndParking != null) {
                for(i in gcd.alMovingAndParking!!.indices) {
                    val mpd = gcd.alMovingAndParking!![i] as GeoPeriodData
                    if(mpd.getState() != 0) continue

                    val tmWorkTime = TreeMap<String, Int>()
                    val hmSCW = objectConfig.hmSensorConfig[SensorConfig.SENSOR_WORK]
                    if(hmSCW != null && !hmSCW.isEmpty()) for(portNum in hmSCW.keys) {
                        val scw = hmSCW[portNum] as SensorConfigWork

                        val alWork = ObjectCalc.calcWorkSensor(alRawTime, alRawData, objectConfig, scw, mpd.begTime, mpd.endTime, mutableListOf()).alWorkOnOff
                        if(alWork != null) for(apd in alWork) if(apd.getState() != 0) {
                            val workTime = tmWorkTime[scw.descr]
                            tmWorkTime[scw.descr] = (workTime ?: 0) + (apd.endTime - apd.begTime)
                        }
                    }

                    //StringBuilder sbParkingTime = StringFunction.MillisInterval_SB(  mpd.begTime, mpd.endTime  );
                    //StringBuilder sbText = new StringBuilder(  sbParkingTime.substring(  0, sbParkingTime.length() - 3  )  );
                    val sbText = secondIntervalToString(mpd.begTime, mpd.endTime)
                    val sbToolTip = StringBuilder(objectNameAndModel).append(sLineSeparator).append("Стоянка с ").append(DateTime_DMYHMS(zoneId, mpd.begTime))
                        .append(" до ").append(DateTime_DMYHMS(zoneId, mpd.endTime)).append(sLineSeparator).append("Всего: ").append(sbText)
                    if(!tmWorkTime.isEmpty()) sbToolTip.append(sLineSeparator).append("Время работы оборудования:")
                    for((descr, wt) in tmWorkTime)
                        sbToolTip.append(sLineSeparator).append(descr).append(": ").append(secondIntervalToString(wt))

                    val objectParking = XyElement(TYPE_OBJECT_PARKING, -getRandomInt(), objectParamData.objectID)
                    //objectParking.init( zoneId )
                    objectParking.itReadOnly = true
                    objectParking.alPoint.add(mpd.parkingCoord!!)
                    objectParking.anchorX = XyElement.Anchor.LT
                    objectParking.anchorY = XyElement.Anchor.LT
                    objectParking.fillColor = 0xFFFFFFFF.toInt()
                    objectParking.drawColor = 0xFF0000FF.toInt()
                    objectParking.lineWidth = 1

                    objectParking.text = sbText
                    objectParking.toolTipText = sbToolTip.toString()
                    objectParking.textColor = 0xFF0000FF.toInt()
                    objectParking.fontSize = iCoreAppContainer.BASE_FONT_SIZE
                    objectParking.itFontBold = true

                    alElement.add(objectParking)
                }
            }
            //--- вывод превышений
            if(gcd.alOverSpeed != null) {
                for(i in gcd.alOverSpeed!!.indices) {
                    val ospd = gcd.alOverSpeed!![i] as OverSpeedPeriodData
                    if(ospd.getState() == 0) continue
                    val sbToolTip = StringBuilder(objectNameAndModel).append(sLineSeparator).append("Превышение с ").append(DateTime_DMYHMS(zoneId, ospd.begTime))
                        .append(" до ").append(DateTime_DMYHMS(zoneId, ospd.endTime)).append(sLineSeparator).append("Всего: ")
                        .append(secondIntervalToString(ospd.begTime, ospd.endTime)).append(sLineSeparator).append("Максимальная скорость на участке: ")
                        .append(ospd.maxOverSpeedMax).append(" км/ч").append(sLineSeparator).append("Максимальное превышение на участке: ")
                        .append(ospd.maxOverSpeedDiff).append(" км/ч")

                    val objectOverSpeed = XyElement(TYPE_OBJECT_OVER_SPEED, -getRandomInt(), objectParamData.objectID)
                    //objectOverSpeed.init( zoneId )
                    objectOverSpeed.itReadOnly = true
                    objectOverSpeed.alPoint.add(ospd.maxOverSpeedCoord!!)
                    objectOverSpeed.anchorX = XyElement.Anchor.LT
                    objectOverSpeed.anchorY = XyElement.Anchor.LT
                    objectOverSpeed.fillColor = 0xFFFFFFFF.toInt()
                    objectOverSpeed.drawColor = 0xFFFF0000.toInt()
                    objectOverSpeed.lineWidth = 1

                    objectOverSpeed.text = StringBuilder().append(ospd.maxOverSpeedDiff).toString()
                    objectOverSpeed.toolTipText = sbToolTip.toString()
                    objectOverSpeed.textColor = 0xFFFF0000.toInt()
                    objectOverSpeed.fontSize = iCoreAppContainer.BASE_FONT_SIZE
                    objectOverSpeed.itFontBold = true

                    alElement.add(objectOverSpeed)
                }
            }
        }

        //--- генерация индикатора текущего положения
        val objectState = ObjectState.getState(stm, objectConfig)

        //--- если текущее положение не в будущем относительно запрашиваемого периода
        //--- и есть последние координаты
        if(objectState.time <= objectParamData.endTime && objectState.pixPoint != null) {
            val sbToolTip = StringBuilder(objectNameAndModel).append(sLineSeparator).append("Время: ").append(DateTime_DMYHMS(zoneId, objectState.time))
            if(objectConfig.scg!!.isUseSpeed) sbToolTip.append(sLineSeparator).append("Скорость: ").append(objectState.speed).append(" км/ч")
            for(descr in objectState.tmWorkState.keys) {
                val state = objectState.tmWorkState[descr]
                sbToolTip.append(sLineSeparator).append(descr).append(": ").append(if(state == null) "( неизв. )" else if(state) "ВКЛ." else "выкл.")
            }
            for(descr in objectState.tmLiquidLevel.keys) {
                val error = objectState.tmLiquidError[descr]
                val value = objectState.tmLiquidLevel[descr]
                val dim = objectState.tmLiquidDim[descr]
                sbToolTip.append(sLineSeparator).append(descr).append(": ").append(error ?: if(value == null) "( неизв. )" else getSplittedDouble(value, 0).append(' ').append(dim).toString())
            }
            val objectInfo = XyElement(TYPE_OBJECT_INFO, -getRandomInt(), objectParamData.objectID)
            //objectInfo.init( zoneId )
            objectInfo.itReadOnly = true
            objectInfo.alPoint.add(objectState.pixPoint!!)
            objectInfo.toolTipText = sbToolTip.toString()

            objectInfo.markerType =
                if(objectConfig.scg!!.isUseSpeed || objectConfig.scg!!.isUseRun) XyElement.MarkerType.ARROW else XyElement.MarkerType.DIAMOND
            objectInfo.markerSize = 16
            //--- раскраска а/м на карте в зависимости от времени отсутствия данных
            if(objectConfig.isDisabled) {
                objectInfo.drawColor = 0xFF000000.toInt()
                objectInfo.fillColor = 0xFF808080.toInt()
            } else if(getCurrentTimeInt() - objectState.time > ObjectConfig.CRITICAL_TIME) {
                objectInfo.drawColor = 0xFF000000.toInt()
                objectInfo.fillColor = 0xFFFF0000.toInt()
            } else if(getCurrentTimeInt() - objectState.time > ObjectConfig.WARNING_TIME) {
                objectInfo.drawColor = 0xFF000000.toInt()
                objectInfo.fillColor = 0xFFFFFF00.toInt()
            } else {
                objectInfo.drawColor = 0xFF000000.toInt()
                objectInfo.fillColor = 0xFF00FF00.toInt()
            }
            objectInfo.lineWidth = 2
            //--- если угол поворота определить все равно не удалось ( нет траектории и только одна точка cur_pos )
            objectInfo.rotateDegree = if(objectState.objectAngle == null) 0.0 else objectState.objectAngle!!

            alElement.add(objectInfo)

            //                for(  Integer sensorType : tmSensorStatus.keySet()  ) {
            //                    SensorDInfo si = SensorDInfo.tmSensorDInfo.get(  sensorType  );
            //                    autoInfo.setAutoDSensor(  sensorType, new StringBuilder(  si.descr  )
            //                            .append(  ": "  ).append(  tmSensorStatus.get(  sensorType  ) ? si.descrOn : si.descrOff  )  );
            //                }
            //    public static final int POWER_STATUS_NONE = 0;
            //    public static final int POWER_STATUS_MASTER = 1;
            //    public static final int POWER_STATUS_RESERVE = 2;
            //    public static final int POWER_STATUS_DISCHARGED = 3;
            //    public static HashMap<Integer,String> hmPowerStatus = new HashMap<>();
            //    static {
            //        hmPowerStatus.put(  POWER_STATUS_NONE, "( неизвестно )"  );
            //        hmPowerStatus.put(  POWER_STATUS_MASTER, "Основное"  );
            //        hmPowerStatus.put(  POWER_STATUS_RESERVE, "Резервное"  );
            //        hmPowerStatus.put(  POWER_STATUS_DISCHARGED, "Резервное ( батарея разряжена )"  );
            //    }
        }
    }

    //-------------------------------------------------------------------------------------------------------------

    protected fun getMinMaxCoords(pixPoint: XyPoint, minXY: XyPoint, maxXY: XyPoint) {
        minXY.x = min(minXY.x, pixPoint.x)
        minXY.y = min(minXY.y, pixPoint.y)
        maxXY.x = max(maxXY.x, pixPoint.x)
        maxXY.y = max(maxXY.y, pixPoint.y)
    }
}

//!!!MAP
//    protected byte[] doLinkToParentObject(  Connection[  ] alConn, HashMap<String,Object> hmParam  ) throws Throwable {
//
//        Integer objectID = ( Integer ) hmParam.get(  XyParameter.OBJECT_ID  );
//        Statement stm = DBFunction.createStatement(  alConn.get(  0  )  );
//        semZone.linkToRoute(  alConn, stm, hmParam, ( Integer ) hmParam.get(  XyParameter.PARENT_OBJECT_ID  ), objectID  );
//        stm.close();
//
//        String xyStartDataID = ( String ) hmParam.get(  XyParameter.XY_START_DATA  );
//        XyStartData sd = ( XyStartData ) hmParam.get(  XyParameter.XY_START_DATA + xyStartDataID  );
//        for(  XyStartObjectData sod : sd.alStartObjectData  )
//            if(  sod.objectID == objectID  ) {
//                sod.isStart = true;
//                break;
//            }
//
//        return null;
//    }
//
//    protected byte[] doUnlinkFromParentObject(  Connection[  ] alConn, HashMap<String,Object> hmParam  ) throws Throwable {
//        int routeID = ( Integer ) hmParam.get(  XyParameter.PARENT_OBJECT_ID  );
//        HashSet<Integer> hsZoneID = ( HashSet<Integer> ) hmParam.get(  XyParameter.OBJECT_ID  );
//        Statement stm = DBFunction.createStatement(  alConn.get(  0  )  );
//        for(  Integer zoneID : hsZoneID  ) semZone.unlinkFromRoute(  stm, routeID, zoneID  );
//        stm.close();
//
//        String xyStartDataID = ( String ) hmParam.get(  XyParameter.XY_START_DATA  );
//        XyStartData sd = ( XyStartData ) hmParam.get(  XyParameter.XY_START_DATA + xyStartDataID  );
//        for(  XyStartObjectData sod : sd.alStartObjectData  )
//            if(  hsZoneID.contains(  sod.objectID  )  )
//                sod.isStart = false;
//
//        return null;
//    }


package foatto.mms.core_mms.graphic.server.document

import foatto.core.app.chart.*
import foatto.core.app.xy.XyProjection
import foatto.util.AdvancedByteBuffer
import foatto.core_server.app.AppParameter
import foatto.core_server.app.graphic.server.ChartStartData
import foatto.core_server.app.graphic.server.document.sdcAbstractGraphic
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.ZoneData
import foatto.mms.core_mms.ZoneLimitData
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.iMMSApplication
import kotlin.math.abs
import kotlin.math.max

class sdcSpeed : sdcAbstractGraphic() {

    override fun doGetElements(chartActionRequest: ChartActionRequest): ChartActionResponse {
        //long begTime = System.currentTimeMillis();

        //--- придёт null, ибо нет необходимости
        //MMSGraphicDocumentConfig mmsgdc = ( MMSGraphicDocumentConfig ) MMSGraphicDocumentConfig.hmConfig.get(  documentTypeName  );
        //int sensorType = mmsgdc.sensorType;
        //AnalogGraphicHandler agh = ( AnalogGraphicHandler ) mmsgdc.graphicHandler;

        val graphicStartDataID = chartActionRequest.startParamId
        val sd = chmSession[AppParameter.CHART_START_DATA + graphicStartDataID] as ChartStartData

        val x1 = chartActionRequest.graphicCoords!!.first
        val x2 = chartActionRequest.graphicCoords!!.second
        val viewWidth = chartActionRequest.viewSize!!.first
        val viewHeight = chartActionRequest.viewSize!!.second

        val maxEnabledOverSpeed = (application as iMMSApplication).maxEnabledOverSpeed

        //--- загрузить данные по зонам
        val hmZoneData = ZoneData.getZoneData(conn, userConfig, 0)

        val oc = (application as iMMSApplication).getObjectConfig(userConfig, sd.objectId)
        //--- загрузка заголовочной информации по объекту
        val sbObjectInfo = StringBuilder(oc.name)

        if (oc.model.isNotEmpty()) {
            sbObjectInfo.append(", ").append(oc.model)
        }
        if (oc.groupName.isNotEmpty()) {
            sbObjectInfo.append(", ").append(oc.groupName)
        }
        if (oc.departmentName.isNotEmpty()) {
            sbObjectInfo.append(", ").append(oc.departmentName)
        }

        val tmElement = sortedMapOf<String, ChartGroupDTO>()
        val tmElementVisibleConfig = sortedMapOf<String, Triple<String, String, Boolean>>()
        if (oc.scg?.isUseSpeed == true) {
            //--- заранее заполняем список опеределений видимости графиков
            val graphicVisibilityKey = "$UP_GRAPHIC_VISIBLE${sd.objectId}_19"
            val isGraphicVisible = userConfig.getUserProperty(graphicVisibilityKey)?.toBooleanStrictOrNull() ?: true
            tmElementVisibleConfig[oc.scg!!.descr] = Triple(oc.scg!!.descr, graphicVisibilityKey, isGraphicVisible)

            //--- а сейчас уже можно и нужно проверять на видимость графика
            if (isGraphicVisible) {
                val hmZoneLimit = ZoneLimitData.getZoneLimit(
                    conn = conn,
                    userConfig = userConfig,
                    objectConfig = oc,
                    hmZoneData = hmZoneData,
                    zoneType = ZoneLimitData.TYPE_LIMIT_SPEED
                )
                val alZoneSpeedLimit = hmZoneLimit[ZoneLimitData.TYPE_LIMIT_SPEED]

                //--- единоразово загрузим данные по объекту
                val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(conn, oc, x1, x2)

                //--- Типовой размер массива = кол-во точек по горизонтали = 3840 ( максимальная ширина экрана ), пусть будет 4000
                //--- Если включён показ линий и выключено сглаживание, то точки можно не показывать,
                //--- их всё равно не будет видно за покрывающей их линией
                val aMaxLimit = ChartElementDTO(ChartElementTypeDTO.LINE, 0, 1, false)
                val aLine = ChartElementDTO(ChartElementTypeDTO.LINE, 0, 3, false)
                //--- показывать график пробега только чистому админу -
                //--- в отладочных целях для быстрого поиска точек с неправильными пробегами
                val aDistance = if (userConfig.isCleanAdmin) {
                    ChartElementDTO(ChartElementTypeDTO.LINE, 1, 1, false)
                } else {
                    null
                }
                val aZone = ChartElementDTO(ChartElementTypeDTO.TEXT, 0, 0, false)

                getSmoothSpeedGraphicData(
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    oc = oc,
                    begTime = x1,
                    endTime = x2,
                    xScale = if (viewWidth == 0) {
                        0
                    } else {
                        (x2 - x1) / viewWidth
                    },
                    yScale = if (viewHeight == 0) {
                        0.0
                    } else {
                        200.0 / viewHeight
                    },
                    maxEnabledOverSpeed = maxEnabledOverSpeed,
                    alZoneSpeedLimit = alZoneSpeedLimit,
                    hmZoneData = hmZoneData,
                    aMaxLimit = aMaxLimit,
                    aSpeed = aLine,
                    aDistance = aDistance,
                    aZone = aZone
                )

                val alAxisYData = mutableListOf<ChartAxisYDTO>()
                alAxisYData.add(ChartAxisYDTO("${SensorConfig.hmSensorDescr[SensorConfig.SENSOR_GEO]}, [ км/ч ]", 0.0f, 200.0f, ChartColorIndex.AXIS_0, false))
                //--- динамическая верхняя граница
                if (aDistance != null) {
                    var maxDistance = 0.0
                    for (gld in aDistance.alGLD) {
                        maxDistance = max(maxDistance, gld.y.toDouble())
                    }
                    alAxisYData.add(ChartAxisYDTO("Пробег в точке, [ м ]", 0.0f, maxDistance.toFloat(), ChartColorIndex.AXIS_1, false))
                }

                val ge = ChartGroupDTO(
                    chartTitle = oc.scg!!.descr,
                    alLegend = emptyList(),
                    chartHeight = -1.0f,
                    alAxisYData = alAxisYData,
                    alGDC = listOfNotNull(aZone, aDistance, aMaxLimit, aLine).filter { it.isNotEmpty() }
                )

                tmElement[ge.chartTitle] = ge
            }
        }
        //AdvancedLogger.debug(  "Graphic time [ ms ] = " + (  System.currentTimeMillis() - begTime  )  );
        //AdvancedLogger.debug(  "------------------------------------------------------------"  );

        return ChartActionResponse(
            hmIndexColor = hmIndexColor,
            alCharts = tmElement.toList(),
            alVisibleElement = tmElementVisibleConfig.values.toList(),
            alLegend = emptyList(),
        )

    }

    //--- сглаживание графика аналоговой величины
    private fun getSmoothSpeedGraphicData(
        alRawTime: List<Int>,
        alRawData: List<AdvancedByteBuffer>,
        oc: ObjectConfig,
        begTime: Int,
        endTime: Int,
        xScale: Int,
        yScale: Double,
        maxEnabledOverSpeed: Int,
        alZoneSpeedLimit: List<ZoneLimitData>?,
        hmZoneData: Map<Int, ZoneData>,
        aMaxLimit: ChartElementDTO,
        aSpeed: ChartElementDTO,
        aDistance: ChartElementDTO?,
        aZone: ChartElementDTO?
    ) {

        val hmCurZone = mutableMapOf<String, Int>()
        val hsZoneForDelete = mutableSetOf<String>()

        var rawTime = 0

        val aMaxLimitGLD = aMaxLimit.alGLD.toMutableList()
        val aSpeedGLD = aSpeed.alGLD.toMutableList()
        val aDistanceGLD = aDistance?.run {
            alGLD.toMutableList()
        } ?: mutableListOf()

        val alZoneGTD = aZone?.run {
            alGTD.toMutableList()
        } ?: mutableListOf()

        for (pos in alRawTime.indices) {
            rawTime = alRawTime[pos]
            //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
            if (rawTime < begTime) continue
            if (rawTime > endTime) break

            val gd = AbstractObjectStateCalc.getGeoData(oc.scg!!, alRawData[pos]) ?: continue
            //--- самих геоданных может и не оказаться

            val pixPoint = XyProjection.wgs_pix(gd.wgs)

            val gldLast = if (aSpeed.alGLD.isEmpty()) null else aSpeed.alGLD[aSpeed.alGLD.size - 1]

            val overSpeed = ObjectCalc.calcOverSpeed(oc.scg!!.maxSpeedLimit, alZoneSpeedLimit, pixPoint, gd.speed)
            val curColorIndex = if (overSpeed > maxEnabledOverSpeed) ChartColorIndex.LINE_ABOVE_0
            else ChartColorIndex.LINE_NORMAL_0
            //--- нереализуемо - т.к. предыдущая точка всегда будет нормальной, т.к. предыдущая к ней точка тоже была нормальной
            //--- и т.д. по рекурсии
            //byte newColorIndex = gldLast == null || gldLast.colorIndex == GraphicColorIndex.LINE_NORMAL_0
            //                    ? GraphicColorIndex.LINE_NORMAL_0 : curColorIndex;

            if (gldLast == null || rawTime - gldLast.x > xScale || abs(gd.speed - gldLast.y) > yScale || curColorIndex != gldLast.colorIndex) {

                aMaxLimitGLD.add(ChartElementLineDTO(rawTime, (gd.speed - overSpeed).toFloat(), ChartColorIndex.LINE_LIMIT))
                aSpeedGLD.add(ChartElementLineDTO(rawTime, gd.speed.toFloat(), curColorIndex))
            }

            //--- отдельно пишем "график" пробега для вылавливания глючных пробегов:
            //--- - точки с нулевым пробегом пропускаем
            //--- - "лишние" с точки зрения x/y-масштабирования точки не пропускаем
            if (aDistance != null && gd.distance > 0) {
                aDistanceGLD.add(ChartElementLineDTO(rawTime, gd.distance.toFloat(), ChartColorIndex.LINE_NORMAL_1))
            }

            aZone?.let {
                //--- пишем список зон ---
                //--- составление списка зон для данной точки ( работаем с name, а не ID, для удобства последующей группировки/сортировки )
                val hsZoneName = mutableSetOf<String>()
                for (zd in hmZoneData.values) {
                    if (zd.polygon!!.isContains(pixPoint)) hsZoneName.add(zd.name)
                }
                //--- по каждой обрабатываемой сейчас зоне
                hsZoneForDelete.clear()
                for (zoneName in hmCurZone.keys) {
                    //--- мы всё ещё находимся в этой зоне - убрать обработанную зону из списка
                    if (!hsZoneName.remove(zoneName)) {
                        //--- этой зоны уже нет в списка - мы вышли из  неё
                        alZoneGTD += ChartElementTextDTO(
                            textX1 = hmCurZone[zoneName]!!,
                            textX2 = rawTime / 1000,
                            fillColorIndex = ChartColorIndex.FILL_NEUTRAL,
                            borderColorIndex = ChartColorIndex.BORDER_NEUTRAL,
                            textColorIndex = ChartColorIndex.TEXT_NEUTRAL,
                            text = zoneName,
                            toolTip = zoneName
                        )
                        //--- нельзя удалить элемент из итерируемого списка, приходится сохранять список удалемых зон отдельно
                        hsZoneForDelete.add(zoneName)
                    }
                }
                //--- удаляем обработанные зоны
                for (zoneName in hsZoneForDelete) {
                    hmCurZone.remove(zoneName)
                }
                //--- обработка оставшихся в списке новых зон
                for (zoneName in hsZoneName) {
                    hmCurZone[zoneName] = rawTime
                }
            }
        }
        //--- в конце периода закроем/сохранием незакрытые зоны
        aZone?.let {
            for (zoneName in hmCurZone.keys) {
                alZoneGTD += ChartElementTextDTO(
                    textX1 = hmCurZone[zoneName]!!,
                    textX2 = rawTime,
                    fillColorIndex = ChartColorIndex.FILL_NEUTRAL,
                    borderColorIndex = ChartColorIndex.BORDER_NEUTRAL,
                    textColorIndex = ChartColorIndex.TEXT_NEUTRAL,
                    text = zoneName,
                    toolTip = zoneName
                )
            }
            aZone.alGTD = alZoneGTD
        }

        aMaxLimit.alGLD = aMaxLimitGLD
        aSpeed.alGLD = aSpeedGLD
        aDistance?.apply {
            alGLD = aDistanceGLD
        }
    }
}
